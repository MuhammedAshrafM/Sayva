"""Fallback temporal-LSTM TFLite exporter: weight transfer PyTorch → Keras.

`to_tflite_temporal_windows.py` (onnx2tf path) fails on LSTM Expand ops.
This module bypasses ONNX entirely: rebuild the same architecture in Keras,
copy the trained weights over element-by-element, then use
`tf.lite.TFLiteConverter` (which handles LSTM natively via TF's built-in
TFLite converter). This is the fallback path explicitly named in the plan.

### Weight layout parity notes

PyTorch's `nn.LSTM` and Keras's `LSTM` both use the (input, forget, cell,
output) gate ordering — so gate-order remapping is a no-op. The only
transformations we apply:

  * `weight_ih_l[k]` → `.T` → Keras `kernel`
    (PyTorch: `[4*hidden, input_size]`; Keras: `[input_size, 4*hidden]`)
  * `weight_hh_l[k]` → `.T` → Keras `recurrent_kernel`
  * `bias_ih_l[k] + bias_hh_l[k]` → Keras `bias`
    (PyTorch has two biases per gate; Keras collapses them into one.
    Since forward pass is `y = Wx + b_ih + Ux + b_hh`, adding them is
    algebraically identical.)
  * Final `nn.Linear.weight.T` → Keras `Dense` kernel; bias passes through.

The included parity check catches any layout mistake by comparing
PyTorch vs Keras vs TFLite outputs on random inputs; a failure aborts the
export so a subtly-broken model can't ship.
"""

from __future__ import annotations

import argparse
import os
from pathlib import Path

import numpy as np
import torch

from sayva_ml.models.temporal_lstm import (
    INPUT_FEATURES,
    TemporalSignLstm,
)

# Silence TensorFlow's chattier warnings — must be set before `import tensorflow`.
os.environ.setdefault("TF_CPP_MIN_LOG_LEVEL", "2")

_ML_ROOT = Path(__file__).resolve().parents[3]
_REPO_ROOT = _ML_ROOT.parent


def _pack_export_root(pack_code: str) -> Path:
    return _ML_ROOT / "packs" / pack_code / "models" / "exported"


def _build_keras_model(
    num_classes: int,
    sequence_length: int,
    hidden_size: int,
):
    """Same architecture as `TemporalSignLstm`, expressed in Keras."""
    import tensorflow as tf

    inputs = tf.keras.Input(shape=(sequence_length, INPUT_FEATURES), batch_size=1)
    # First LSTM: returns sequences so we can stack.
    # `unroll=True` unfolds the RNN into a static graph of `sequence_length`
    # timesteps. Necessary because the default (dynamic) form lowers to
    # TensorList ops that only work with the Flex TFLite delegate — that
    # would require shipping `tensorflow-lite-select-tf-ops` on Android
    # (~10 MB APK bloat) and lack an iOS equivalent. Unrolled is slower to
    # train but our sequence length is a fixed 30 anyway.
    x = tf.keras.layers.LSTM(
        hidden_size,
        return_sequences=True,
        unit_forget_bias=False,
        unroll=True,
        name="lstm_layer_0",
    )(inputs)
    x = tf.keras.layers.LSTM(
        hidden_size,
        return_sequences=False,
        unit_forget_bias=False,
        unroll=True,
        name="lstm_layer_1",
    )(x)
    logits = tf.keras.layers.Dense(num_classes, name="head")(x)
    return tf.keras.Model(inputs, logits, name="temporal_sign_lstm")


def _transfer_weights(
    torch_model: TemporalSignLstm,
    keras_model,  # tf.keras.Model
) -> None:
    state = torch_model.state_dict()

    def _lstm_weights(layer_idx: int) -> list[np.ndarray]:
        w_ih = state[f"lstm.weight_ih_l{layer_idx}"].detach().cpu().numpy()
        w_hh = state[f"lstm.weight_hh_l{layer_idx}"].detach().cpu().numpy()
        b_ih = state[f"lstm.bias_ih_l{layer_idx}"].detach().cpu().numpy()
        b_hh = state[f"lstm.bias_hh_l{layer_idx}"].detach().cpu().numpy()
        # Keras LSTM.set_weights order: [kernel, recurrent_kernel, bias]
        return [w_ih.T.astype(np.float32), w_hh.T.astype(np.float32), (b_ih + b_hh).astype(np.float32)]

    keras_model.get_layer("lstm_layer_0").set_weights(_lstm_weights(0))
    keras_model.get_layer("lstm_layer_1").set_weights(_lstm_weights(1))

    head_w = state["head.weight"].detach().cpu().numpy()
    head_b = state["head.bias"].detach().cpu().numpy()
    keras_model.get_layer("head").set_weights(
        [head_w.T.astype(np.float32), head_b.astype(np.float32)],
    )


def _pytorch_infer(model: TemporalSignLstm, x: np.ndarray) -> np.ndarray:
    model.eval()
    with torch.inference_mode():
        out = model(torch.from_numpy(x)).numpy()
    return out


def _tflite_infer(interpreter, x: np.ndarray) -> np.ndarray:
    inp = interpreter.get_input_details()[0]
    out = interpreter.get_output_details()[0]
    interpreter.set_tensor(inp["index"], x)
    interpreter.invoke()
    return interpreter.get_tensor(out["index"])


def convert(  # noqa: PLR0913
    version: str,
    num_classes: int,
    sequence_length: int,
    hidden_size: int,
    pack: str = "ase",
    n_probes: int = 8,
    tolerance: float = 1e-3,
) -> Path:
    import tensorflow as tf
    from ai_edge_litert.interpreter import Interpreter  # type: ignore[import-not-found]

    version_dir = _pack_export_root(pack) / version
    checkpoint = version_dir / "model.pt"
    if not checkpoint.exists():
        raise SystemExit(
            f"Missing {checkpoint}. Run train_temporal.py --version {version} first."
        )

    torch_model = TemporalSignLstm(num_classes=num_classes, hidden_size=hidden_size)
    torch_model.load_state_dict(torch.load(checkpoint, map_location="cpu"))
    torch_model.eval()

    keras_model = _build_keras_model(
        num_classes=num_classes,
        sequence_length=sequence_length,
        hidden_size=hidden_size,
    )
    _transfer_weights(torch_model, keras_model)

    # Parity check 1: PyTorch vs Keras on random inputs — catches weight
    # transpose or gate-order bugs immediately.
    rng = np.random.default_rng(seed=0)
    max_torch_keras_diff = 0.0
    for _ in range(n_probes):
        x = rng.standard_normal((1, sequence_length, INPUT_FEATURES)).astype(np.float32)
        torch_out = _pytorch_infer(torch_model, x)
        keras_out = keras_model.predict(x, verbose=0)
        max_torch_keras_diff = max(
            max_torch_keras_diff,
            float(np.abs(torch_out - keras_out).max()),
        )
    print(f"pytorch vs keras: max abs diff = {max_torch_keras_diff:.2e}")
    if max_torch_keras_diff > tolerance:
        raise SystemExit(
            f"Weight transfer failed — PyTorch/Keras diverge by "
            f"{max_torch_keras_diff:.2e} > tolerance {tolerance:.2e}. "
            "Aborting before TFLite conversion so the bug is found here, "
            "not on device."
        )

    # Convert to TFLite. With `unroll=True` above the graph is pure built-in
    # TFLite ops — no Flex delegate required, no APK bloat.
    converter = tf.lite.TFLiteConverter.from_keras_model(keras_model)
    tflite_bytes = converter.convert()

    tflite_dst = version_dir / "model.tflite"
    tflite_dst.write_bytes(tflite_bytes)

    # Parity check 2: TFLite vs Keras on the same random inputs.
    interpreter = Interpreter(model_path=str(tflite_dst))
    interpreter.allocate_tensors()
    max_keras_tflite_diff = 0.0
    disagreements = 0
    for _ in range(n_probes):
        x = rng.standard_normal((1, sequence_length, INPUT_FEATURES)).astype(np.float32)
        keras_out = keras_model.predict(x, verbose=0)
        tflite_out = _tflite_infer(interpreter, x)
        diff = float(np.abs(keras_out - tflite_out).max())
        max_keras_tflite_diff = max(max_keras_tflite_diff, diff)
        if int(keras_out.argmax()) != int(tflite_out.argmax()):
            disagreements += 1
    print(
        f"keras vs tflite: max abs diff = {max_keras_tflite_diff:.2e}, "
        f"argmax disagreements = {disagreements}/{n_probes}"
    )
    if disagreements > 0 or max_keras_tflite_diff > tolerance:
        raise SystemExit(
            f"TFLite conversion failed parity check — diff={max_keras_tflite_diff:.2e}, "
            f"disagreements={disagreements}. Model is unsafe to ship."
        )

    return tflite_dst


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--version", default="temporal_v1.0.0")
    parser.add_argument("--pack", default="ase")
    parser.add_argument("--num-classes", type=int, default=5)
    parser.add_argument("--sequence-length", type=int, default=30)
    parser.add_argument("--hidden-size", type=int, default=64)
    parser.add_argument("--tolerance", type=float, default=1e-3)
    args = parser.parse_args()

    out = convert(
        version=args.version,
        pack=args.pack,
        num_classes=args.num_classes,
        sequence_length=args.sequence_length,
        hidden_size=args.hidden_size,
        tolerance=args.tolerance,
    )
    print(f"wrote: {out.relative_to(_REPO_ROOT)} ({out.stat().st_size} bytes)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
