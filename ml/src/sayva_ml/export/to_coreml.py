"""Convert a trained fingerspelling MLP to CoreML for iOS.

Path: PyTorch → CoreML via `coremltools.converters.convert(torch_model, ...)`.
`coremltools` is macOS-only for the newer converters — this script requires
running on a Mac (or a CI runner with `macos-latest`).

If you are on Windows/Linux, the trained `.pt` transfers cleanly — commit it,
run this script on a Mac, then commit the `.mlpackage`. The Kotlin/iOS side
picks up the `.mlpackage` from `shared/src/commonMain/composeResources/`.

Run:
    uv run --extra export python -m sayva_ml.export.to_coreml \\
        --version fingerspelling_v0.1.0

Requires the `export` extra (`coremltools`).
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

import torch

from sayva_ml.models.fingerspelling_mlp import FingerspellingMLP
from sayva_ml.packs.manifest import load_manifest
from sayva_ml.packs.registry import DEFAULT_PACKS_ROOT

_ML_ROOT = Path(__file__).resolve().parents[3]
_REPO_ROOT = _ML_ROOT.parent


def _pack_export_root(pack_code: str) -> Path:
    return _ML_ROOT / "packs" / pack_code / "models" / "exported"


def convert(version: str) -> Path:
    # `coremltools` 8+ has Windows wheels for the *import* path, but the
    # protobuf `BlobWriter` used to serialize the .mlpackage is a native
    # library shipped only for macOS + Linux (verified: coremltools 9.0 on
    # Windows fails with "BlobWriter not loaded"). Any Windows machine will
    # therefore stop here — commit the `.pt` / `.onnx` and run this script
    # on a Mac, Linux, or WSL runner. The Kotlin/iOS side picks up whichever
    # `.mlpackage` lands in `models/exported/<version>/` regardless of who
    # built it.
    if sys.platform not in ("darwin", "linux"):
        raise SystemExit(
            "CoreML export requires macOS or Linux (coremltools ships its "
            f"BlobWriter native lib only for those platforms; current: {sys.platform}). "
            "Run this on a Mac / Linux runner / WSL after training. "
            "The Kotlin/iOS side loads whatever .mlpackage lands next to model.pt."
        )
    import coremltools as ct  # type: ignore[import-not-found]

    version_dir = _pack_export_root("ase") / version
    checkpoint = version_dir / "model.pt"
    if not checkpoint.exists():
        raise SystemExit(f"Missing {checkpoint}. Train first.")

    pack = load_manifest(DEFAULT_PACKS_ROOT / "ase")
    model_spec = next(m for m in pack.models if m.id == "fingerspelling")
    vocab = model_spec.vocabulary
    model = FingerspellingMLP(num_classes=vocab.size)
    model.load_state_dict(torch.load(checkpoint, map_location="cpu"))
    model.eval()

    sample = torch.zeros(1, 42, dtype=torch.float32)
    traced = torch.jit.trace(model, sample)

    mlmodel = ct.convert(
        traced,
        inputs=[ct.TensorType(name="landmarks", shape=(1, 42))],
        outputs=[ct.TensorType(name="logits")],
        classifier_config=ct.ClassifierConfig([s.id for s in vocab.signs]),
    )
    output = version_dir / "model.mlpackage"
    mlmodel.save(str(output))
    return output


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--version", default="fingerspelling_v0.1.0")
    args = parser.parse_args()

    out = convert(version=args.version)
    print(f"wrote: {out.relative_to(_REPO_ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
