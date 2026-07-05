"""Windows-compatible ONNX → TFLite converter using `onnx2tf`.

`ai-edge-torch` (the modern PyTorch → TFLite bridge) has no Windows wheel for
its `litert-converter` transitive dep. This module works around that by going
via `onnx2tf` (pure Python + TensorFlow), which does have Windows wheels.

Path: PyTorch → ONNX (already emitted by `train_fingerspelling.py`) → TFLite
(this script).

Run:
    uv run --extra export python -m sayva_ml.export.to_tflite_windows \\
        --version fingerspelling_v0.1.0

`to_tflite.py` (the ai-edge-torch variant) remains the preferred path on
Linux/macOS — same interface, better maintained. Both produce a `model.tflite`
in `models/exported/<version>/` and the Kotlin side doesn't care which built it.
"""

from __future__ import annotations

import argparse
import shutil
import tempfile
from pathlib import Path

_ML_ROOT = Path(__file__).resolve().parents[3]
_REPO_ROOT = _ML_ROOT.parent


def _pack_export_root(pack_code: str) -> Path:
    return _ML_ROOT / "packs" / pack_code / "models" / "exported"


def convert(version: str, pack: str = "ase") -> Path:
    import onnx2tf  # type: ignore[import-not-found]

    version_dir = _pack_export_root(pack) / version
    onnx_path = version_dir / "model.onnx"
    if not onnx_path.exists():
        raise SystemExit(f"Missing {onnx_path}. Run train_fingerspelling.py first.")

    # onnx2tf writes several files (saved_model, tflite variants, JSON). We
    # only want the plain float32 .tflite next to our other artifacts.
    with tempfile.TemporaryDirectory() as tmp:
        onnx2tf.convert(
            input_onnx_file_path=str(onnx_path),
            output_folder_path=tmp,
            copy_onnx_input_output_names_to_tflite=True,
            output_signaturedefs=False,
            non_verbose=True,
        )
        # onnx2tf's plain float32 output is <stem>_float32.tflite
        tflite_src = Path(tmp) / "model_float32.tflite"
        if not tflite_src.exists():
            # Older versions emit `saved_model/model.tflite` under different names
            candidates = list(Path(tmp).rglob("*.tflite"))
            if not candidates:
                raise SystemExit(f"onnx2tf produced no .tflite in {tmp}")
            tflite_src = candidates[0]
        tflite_dst = version_dir / "model.tflite"
        shutil.copyfile(tflite_src, tflite_dst)
    return tflite_dst


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--version", default="fingerspelling_v0.1.0")
    parser.add_argument("--pack", default="ase")
    args = parser.parse_args()

    out = convert(version=args.version, pack=args.pack)
    print(f"wrote: {out.relative_to(_REPO_ROOT)} ({out.stat().st_size} bytes)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
