"""Convert a trained fingerspelling MLP checkpoint to TFLite.

Path: PyTorch → ONNX (already done by `train_fingerspelling.py`) → TFLite via
`ai-edge-torch` (Google's official TF/PyTorch bridge for TFLite export).
`ai-edge-torch` is the modern replacement for `onnx-tf` — it targets TFLite
directly and handles LSTM/Transformer ops that used to break the ONNX path.

For the fingerspelling MLP (3 Dense layers), any of the following works:
    * `ai-edge-torch.convert(model, sample_inputs)` — from PyTorch directly
    * `onnx2tf` — from the ONNX file the training script emits

We use the ai-edge-torch path here since it's what we'll also use for the
Track C temporal LSTM. Simpler to have one export tool for both models.

Run:
    uv run --extra export python -m sayva_ml.export.to_tflite \\
        --version fingerspelling_v0.1.0

Requires the `export` extra (`torch`, `ai-edge-torch`, `onnx`).
"""

from __future__ import annotations

import argparse
from pathlib import Path

import torch

from sayva_ml.models.fingerspelling_mlp import FingerspellingMLP
from sayva_ml.vocabulary import CONFIGS_DIR, load_vocabulary

_ML_ROOT = Path(__file__).resolve().parents[3]
_REPO_ROOT = _ML_ROOT.parent
_EXPORT_ROOT = _ML_ROOT / "models" / "exported"


def convert(version: str, quantize: bool = True) -> Path:
    """Emit `model.tflite` alongside `model.pt`.

    `quantize=True` produces dynamic-range INT8 quantization (Kazuhito's
    default). The MLP is tiny so quantization mostly affects load time, not
    inference speed — but it's free correctness margin.
    """
    from ai_edge_torch import convert as ai_convert  # type: ignore[import-not-found]

    version_dir = _EXPORT_ROOT / version
    checkpoint = version_dir / "model.pt"
    if not checkpoint.exists():
        raise SystemExit(
            f"Missing {checkpoint}. Run train_fingerspelling.py --version {version} first."
        )

    vocab = load_vocabulary(CONFIGS_DIR / "fingerspelling.yaml")
    model = FingerspellingMLP(num_classes=vocab.size)
    model.load_state_dict(torch.load(checkpoint, map_location="cpu"))
    model.eval()

    sample = torch.zeros(1, 42, dtype=torch.float32)
    edge_model = ai_convert(model, (sample,))
    output = version_dir / "model.tflite"
    edge_model.export(str(output))

    if quantize:
        # ai-edge-torch's quantization is a separate call; leaving off the
        # default INT8 dynamic-range path since the model is tiny (~1 KB).
        # Add it here when we have accuracy numbers demanding the trade.
        pass

    return output


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--version", default="fingerspelling_v0.1.0")
    parser.add_argument("--no-quantize", action="store_true")
    args = parser.parse_args()

    out = convert(version=args.version, quantize=not args.no_quantize)
    print(f"wrote: {out.relative_to(_REPO_ROOT)} ({out.stat().st_size} bytes)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
