"""Windows-compatible temporal LSTM ONNX → TFLite converter.

Reuses `onnx2tf` — same tool as the fingerspelling `to_tflite_windows.py`,
but LSTM ops are historically the friction point in ONNX → TF → TFLite
pipelines. This script preserves the standard onnx2tf call but adds:

    * `disable_group_convolution` is off (LSTM doesn't use it)
    * We check the resulting `.tflite` against ONNX runtime immediately — if
      the argmax drifts by more than 0 or the max-abs numeric diff exceeds
      1e-3 on 8 random test inputs, we fail loud rather than shipping a
      subtly-broken model.

If this converter fails on Windows (LSTM ops can be finicky), fall back to
converting on Linux/macOS via `ai-edge-torch` — same input `.onnx`, different
converter, same output shape.
"""

from __future__ import annotations

import argparse
import shutil
import tempfile
from pathlib import Path

import numpy as np

_ML_ROOT = Path(__file__).resolve().parents[3]
_REPO_ROOT = _ML_ROOT.parent
_EXPORT_ROOT = _ML_ROOT / "models" / "exported"


def _pick_tflite(directory: Path) -> Path:
    """Prefer `model_float32.tflite` (onnx2tf's canonical name); else any."""
    preferred = directory / "model_float32.tflite"
    if preferred.exists():
        return preferred
    candidates = list(directory.rglob("*.tflite"))
    if not candidates:
        raise SystemExit(f"onnx2tf produced no .tflite in {directory}")
    return candidates[0]


def convert(version: str, check_tolerance: float = 1e-3) -> Path:
    import onnx2tf  # type: ignore[import-not-found]
    import onnxruntime as ort
    from ai_edge_litert.interpreter import Interpreter  # type: ignore[import-not-found]

    version_dir = _EXPORT_ROOT / version
    onnx_path = version_dir / "model.onnx"
    if not onnx_path.exists():
        raise SystemExit(f"Missing {onnx_path}. Run train_temporal.py first.")

    with tempfile.TemporaryDirectory() as tmp:
        onnx2tf.convert(
            input_onnx_file_path=str(onnx_path),
            output_folder_path=tmp,
            copy_onnx_input_output_names_to_tflite=True,
            output_signaturedefs=False,
            non_verbose=True,
        )
        tflite_src = _pick_tflite(Path(tmp))
        tflite_dst = version_dir / "model.tflite"
        shutil.copyfile(tflite_src, tflite_dst)

    # Sanity check: ONNX vs TFLite argmax + max abs diff on random inputs.
    onnx_sess = ort.InferenceSession(str(onnx_path), providers=["CPUExecutionProvider"])
    tflite = Interpreter(model_path=str(tflite_dst))
    tflite.allocate_tensors()
    inp = tflite.get_input_details()[0]
    out = tflite.get_output_details()[0]

    rng = np.random.default_rng(seed=123)
    input_shape = list(inp["shape"])
    n_probe = 8
    max_diff = 0.0
    disagreements = 0
    for _ in range(n_probe):
        x = rng.standard_normal(input_shape).astype(np.float32)
        onnx_out = onnx_sess.run(None, {onnx_sess.get_inputs()[0].name: x})[0]
        tflite.set_tensor(inp["index"], x)
        tflite.invoke()
        tf_out = tflite.get_tensor(out["index"])
        diff = float(np.abs(onnx_out - tf_out).max())
        max_diff = max(max_diff, diff)
        if int(onnx_out.argmax()) != int(tf_out.argmax()):
            disagreements += 1

    print(f"onnx vs tflite: max abs diff = {max_diff:.2e}, argmax disagreements = {disagreements}/{n_probe}")
    if disagreements > 0 or max_diff > check_tolerance:
        raise SystemExit(
            f"Parity check failed — max_diff={max_diff:.2e}, disagreements={disagreements}. "
            f"Threshold {check_tolerance:.2e}. LSTM export may be corrupt; do not ship."
        )
    return tflite_dst


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--version", default="sign_toy_v1.0.0")
    parser.add_argument(
        "--tolerance",
        type=float,
        default=1e-3,
        help="Max abs diff allowed between ONNX and TFLite (per element).",
    )
    args = parser.parse_args()

    out = convert(version=args.version, check_tolerance=args.tolerance)
    print(f"wrote: {out.relative_to(_REPO_ROOT)} ({out.stat().st_size} bytes)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
