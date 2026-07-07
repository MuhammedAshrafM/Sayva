"""Train the fingerspelling MLP.

Two data sources:
    * `--data synthetic`   Generates the pipeline-shape dataset from
                           `sayva_ml.data.synthetic`. Runs anywhere,
                           produces a model that hits >99% on synthetic
                           test data (useless in production, useful for
                           end-to-end pipeline validation).
    * `--data asl-alphabet` Loads the cached ASL Alphabet Dataset from
                           `models/cache/asl_alphabet.npz`, which the
                           user builds via `scripts/build_asl_cache.py`
                           after downloading from Kaggle.

Outputs a versioned artifact directory:
    ml/models/exported/<version>/
        model.pt        — PyTorch checkpoint (inference-only)
        model.onnx      — Portable graph for downstream TFLite/CoreML export
        vocabulary.json — Class labels in output-index order (train/serve check)
        metrics.json    — Train / val / test accuracy for the record
        config.json     — Hyperparams + git SHA + dataset + seed for repro

Convention: version string is `fingerspelling_v<major>.<minor>.<patch>`. Bump
the minor when hyperparams or vocab change, patch on retrains.
"""

from __future__ import annotations

import argparse
import json
import subprocess
import time
from dataclasses import dataclass
from pathlib import Path

import numpy as np
import torch
from torch import nn
from torch.utils.data import DataLoader, TensorDataset

from sayva_ml.data.synthetic import SyntheticSplit, make_split
from sayva_ml.models.fingerspelling_mlp import FingerspellingMLP, parameter_count
from sayva_ml.packs.data_loader import load_pack_data_module
from sayva_ml.packs.manifest import LanguagePackManifest, load_manifest
from sayva_ml.packs.registry import DEFAULT_PACKS_ROOT

# `parents[3]` is `ml/`, `parents[4]` is the repo root.
_ML_ROOT = Path(__file__).resolve().parents[3]
_REPO_ROOT = _ML_ROOT.parent
_CACHE_ROOT = _ML_ROOT / "models" / "cache"


def _pack_export_root(pack_code: str) -> Path:
    return _ML_ROOT / "packs" / pack_code / "models" / "exported"


@dataclass(frozen=True)
class Split:
    """Framework-agnostic dataset shape — same fields as SyntheticSplit /
    RealDatasetSplit so `train()` doesn't care where the data came from."""

    X_train: np.ndarray
    y_train: np.ndarray
    X_val: np.ndarray
    y_val: np.ndarray
    X_test: np.ndarray
    y_test: np.ndarray


def _to_split(src: object) -> Split:
    return Split(
        X_train=src.X_train,  # type: ignore[attr-defined]
        y_train=src.y_train,  # type: ignore[attr-defined]
        X_val=src.X_val,  # type: ignore[attr-defined]
        y_val=src.y_val,  # type: ignore[attr-defined]
        X_test=src.X_test,  # type: ignore[attr-defined]
        y_test=src.y_test,  # type: ignore[attr-defined]
    )


def _load_data(source: str, num_classes: int, samples_per_class: int, seed: int) -> Split:
    if source == "synthetic":
        return _to_split(
            make_split(
                num_classes=num_classes,
                samples_per_class=samples_per_class,
                seed=seed,
            )
        )
    if source == "asl-alphabet":
        # Pack-specific dataset loader lives inside packs/ase/data/. Loaded
        # dynamically via importlib so `sayva_ml/` stays language-neutral.
        asl_module = load_pack_data_module(DEFAULT_PACKS_ROOT / "ase", "asl_alphabet")

        # Read the cache from the pack tree — matches where
        # `build_asl_cache.py` writes it. The legacy `ml/models/cache/` path
        # was a shared holdover from before pack-owned data trees; the
        # workflow doc (docs/PACKS_WORKFLOW.md) makes per-pack ownership the
        # rule.
        cache = DEFAULT_PACKS_ROOT / "ase" / "data" / "cache" / "asl_alphabet.npz"
        if not cache.exists():
            raise SystemExit(
                f"ASL Alphabet cache not found at {cache}. Build it first with:\n"
                f"    uv run python scripts/build_asl_cache.py"
            )
        return _to_split(asl_module.load_cache(cache))
    raise ValueError(f"Unknown data source: {source}")


def _load_pack(pack_code: str) -> LanguagePackManifest:
    return load_manifest(DEFAULT_PACKS_ROOT / pack_code)


def _accuracy(model: nn.Module, X: torch.Tensor, y: torch.Tensor) -> float:
    was_training = model.training
    model.eval()
    with torch.inference_mode():
        logits = model(X)
        preds = logits.argmax(dim=-1)
        acc = (preds == y).float().mean().item()
    if was_training:
        model.train()
    return acc


def train_one(  # noqa: PLR0913
    split: Split,
    num_classes: int,
    epochs: int,
    batch_size: int,
    lr: float,
    seed: int,
    patience: int,
) -> tuple[FingerspellingMLP, dict[str, float]]:
    torch.manual_seed(seed)
    np.random.seed(seed)

    model = FingerspellingMLP(num_classes=num_classes)
    optim = torch.optim.Adam(model.parameters(), lr=lr)
    criterion = nn.CrossEntropyLoss()

    X_train_t = torch.from_numpy(split.X_train)
    y_train_t = torch.from_numpy(split.y_train)
    X_val_t = torch.from_numpy(split.X_val)
    y_val_t = torch.from_numpy(split.y_val)
    X_test_t = torch.from_numpy(split.X_test)
    y_test_t = torch.from_numpy(split.y_test)

    loader = DataLoader(
        TensorDataset(X_train_t, y_train_t),
        batch_size=batch_size,
        shuffle=True,
        drop_last=False,
    )

    best_val = -1.0
    best_state: dict[str, torch.Tensor] = {}
    epochs_since_best = 0

    for epoch in range(epochs):
        model.train()
        running = 0.0
        n = 0
        for xb, yb in loader:
            optim.zero_grad(set_to_none=True)
            logits = model(xb)
            loss = criterion(logits, yb)
            loss.backward()
            optim.step()
            running += float(loss) * xb.size(0)
            n += xb.size(0)
        train_loss = running / n
        val_acc = _accuracy(model, X_val_t, y_val_t)
        if val_acc > best_val:
            best_val = val_acc
            best_state = {k: v.detach().clone() for k, v in model.state_dict().items()}
            epochs_since_best = 0
        else:
            epochs_since_best += 1
        if epoch % 10 == 0 or epoch == epochs - 1:
            print(
                f"epoch {epoch:4d} train_loss={train_loss:.4f} "
                f"val_acc={val_acc:.4f} best_val={best_val:.4f}"
            )
        if epochs_since_best >= patience:
            print(f"early stop at epoch {epoch} (patience {patience})")
            break

    model.load_state_dict(best_state)
    test_acc = _accuracy(model, X_test_t, y_test_t)
    return model, {
        "train_final_loss": train_loss,
        "val_best_acc": best_val,
        "test_acc": test_acc,
    }


def _git_sha() -> str | None:
    try:
        out = subprocess.check_output(
            ["git", "rev-parse", "HEAD"],
            cwd=_REPO_ROOT,
            stderr=subprocess.DEVNULL,
        )
        return out.decode().strip()
    except (FileNotFoundError, subprocess.CalledProcessError):
        return None


def _export_onnx(
    model: FingerspellingMLP,
    output_path: Path,
) -> None:
    """Export to ONNX. Both TFLite and CoreML converters consume ONNX.

    Uses `dynamo=False` (TorchScript exporter) — the newer dynamo path pulls
    in `onnxscript` which we don't need for a Sequential MLP. Save the dep.
    """
    model.eval()
    dummy = torch.zeros(1, 42, dtype=torch.float32)
    torch.onnx.export(
        model,
        (dummy,),
        str(output_path),
        input_names=["landmarks"],
        output_names=["logits"],
        opset_version=17,
        dynamic_axes=None,  # fixed batch size of 1 keeps the tflite converter happy
        dynamo=False,
    )


def _write_artifacts(
    model: FingerspellingMLP,
    metrics: dict[str, float],
    args: argparse.Namespace,
    vocab_signs: list[dict[str, object]],
    version: str,
) -> Path:
    out_dir = _pack_export_root(args.pack) / version
    out_dir.mkdir(parents=True, exist_ok=True)

    torch.save(model.state_dict(), out_dir / "model.pt")
    _export_onnx(model, out_dir / "model.onnx")

    (out_dir / "vocabulary.json").write_text(
        json.dumps(vocab_signs, indent=2), encoding="utf-8"
    )
    (out_dir / "metrics.json").write_text(
        json.dumps(metrics, indent=2), encoding="utf-8"
    )
    (out_dir / "config.json").write_text(
        json.dumps(
            {
                "data": args.data,
                "epochs": args.epochs,
                "batch_size": args.batch_size,
                "lr": args.lr,
                "seed": args.seed,
                "patience": args.patience,
                "parameters": parameter_count(model),
                "git_sha": _git_sha(),
                "timestamp_utc": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
            },
            indent=2,
        ),
        encoding="utf-8",
    )
    return out_dir


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--data", choices=["synthetic", "asl-alphabet"], default="synthetic")
    parser.add_argument("--epochs", type=int, default=200)
    parser.add_argument("--batch-size", type=int, default=128)
    parser.add_argument("--lr", type=float, default=1e-3)
    parser.add_argument("--patience", type=int, default=20)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument(
        "--samples-per-class",
        type=int,
        default=200,
        help="synthetic data source only",
    )
    parser.add_argument(
        "--version",
        default="fingerspelling_v0.1.0",
        help="Output directory name under packs/{pack}/models/exported/",
    )
    parser.add_argument(
        "--pack",
        default="ase",
        help="Recognition-language pack code (packs/{code}/manifest.yaml).",
    )
    args = parser.parse_args()

    pack = _load_pack(args.pack)
    model_spec = next((m for m in pack.models if m.id == "fingerspelling"), None)
    if model_spec is None:
        raise SystemExit(
            f"Pack '{pack.recognition_code}' has no 'fingerspelling' model. "
            f"Declared models: {[m.id for m in pack.models]}"
        )
    vocab = model_spec.vocabulary
    num_classes = vocab.size
    print(
        f"pack: {pack.recognition_code} v{pack.version} — "
        f"fingerspelling model, {num_classes} classes"
    )

    split = _load_data(
        source=args.data,
        num_classes=num_classes,
        samples_per_class=args.samples_per_class,
        seed=args.seed,
    )
    print(
        f"data: train={len(split.X_train)}, val={len(split.X_val)}, "
        f"test={len(split.X_test)}"
    )

    model, metrics = train_one(
        split=split,
        num_classes=num_classes,
        epochs=args.epochs,
        batch_size=args.batch_size,
        lr=args.lr,
        seed=args.seed,
        patience=args.patience,
    )
    print(f"final: {metrics}")

    vocab_signs = [
        {"index": i, "id": s.id, "label": s.label, "tags": list(s.tags)}
        for i, s in enumerate(vocab.signs)
    ]
    out_dir = _write_artifacts(model, metrics, args, vocab_signs, args.version)
    print(f"wrote: {out_dir.relative_to(_REPO_ROOT)}/")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
