"""Train the temporal-sign LSTM.

Same structure as `train_fingerspelling.py`: framework-agnostic split →
train → ONNX export → versioned artifact directory.

Two data sources:
  * `--data synthetic` — no external data. Uses `synthetic_temporal.make_split`.
    Trains to high accuracy in seconds. Proves the pipeline shape.
  * `--data wlasl` — real WLASL clips. Requires `models/cache/wlasl_toy.npz`,
    which the user builds via `scripts/build_wlasl_cache.py` after downloading
    the dataset. Both dataset splits share the `TemporalSplit`-like shape so
    `train_one` doesn't care.

Outputs land in `ml/models/exported/<version>/` — same layout as the
fingerspelling artifact set.
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

from sayva_ml.data.synthetic_temporal import make_split
from sayva_ml.models.temporal_lstm import (
    DEFAULT_SEQUENCE_LENGTH,
    INPUT_FEATURES,
    TemporalSignLstm,
    parameter_count,
)
from sayva_ml.packs.data_loader import load_pack_data_module
from sayva_ml.packs.manifest import LanguagePackManifest, load_manifest
from sayva_ml.packs.registry import DEFAULT_PACKS_ROOT

_ML_ROOT = Path(__file__).resolve().parents[3]
_REPO_ROOT = _ML_ROOT.parent
_CACHE_ROOT = _ML_ROOT / "models" / "cache"


def _pack_export_root(pack_code: str) -> Path:
    return _ML_ROOT / "packs" / pack_code / "models" / "exported"


def _load_pack(pack_code: str) -> LanguagePackManifest:
    return load_manifest(DEFAULT_PACKS_ROOT / pack_code)




@dataclass(frozen=True)
class Split:
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


def _load_data(
    source: str,
    num_classes: int,
    samples_per_class: int,
    sequence_length: int,
    seed: int,
) -> Split:
    if source == "synthetic":
        return _to_split(
            make_split(
                num_classes=num_classes,
                samples_per_class=samples_per_class,
                sequence_length=sequence_length,
                seed=seed,
            )
        )
    if source == "wlasl":
        wlasl_module = load_pack_data_module(DEFAULT_PACKS_ROOT / "ase", "wlasl")

        cache = _CACHE_ROOT / "wlasl_toy.npz"
        if not cache.exists():
            raise SystemExit(
                f"WLASL cache not found at {cache}. Build it first — see ml/README.md."
            )
        return _to_split(wlasl_module.load_cache(cache))
    raise ValueError(f"Unknown data source: {source}")


def _accuracy(model: nn.Module, X: torch.Tensor, y: torch.Tensor) -> float:
    was_training = model.training
    model.eval()
    with torch.inference_mode():
        preds = model(X).argmax(dim=-1)
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
) -> tuple[TemporalSignLstm, dict[str, float]]:
    torch.manual_seed(seed)
    np.random.seed(seed)

    model = TemporalSignLstm(num_classes=num_classes)
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
    train_loss = float("nan")

    for epoch in range(epochs):
        model.train()
        running = 0.0
        n = 0
        for xb, yb in loader:
            optim.zero_grad(set_to_none=True)
            loss = criterion(model(xb), yb)
            loss.backward()
            optim.step()
            running += float(loss) * xb.size(0)
            n += xb.size(0)
        train_loss = running / max(n, 1)
        val_acc = _accuracy(model, X_val_t, y_val_t)
        if val_acc > best_val:
            best_val = val_acc
            best_state = {k: v.detach().clone() for k, v in model.state_dict().items()}
            epochs_since_best = 0
        else:
            epochs_since_best += 1
        if epoch % 5 == 0 or epoch == epochs - 1:
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
    model: TemporalSignLstm,
    output_path: Path,
    sequence_length: int,
) -> None:
    """Export to ONNX. LSTM export uses the TorchScript exporter — the newer
    dynamo path currently has rough edges around LSTM state serialization."""
    model.eval()
    dummy = torch.zeros(1, sequence_length, INPUT_FEATURES, dtype=torch.float32)
    torch.onnx.export(
        model,
        (dummy,),
        str(output_path),
        input_names=["sequence"],
        output_names=["logits"],
        opset_version=17,
        dynamic_axes=None,
        dynamo=False,
    )


def _write_artifacts(
    model: TemporalSignLstm,
    metrics: dict[str, float],
    args: argparse.Namespace,
    vocab_signs: list[dict[str, object]],
    sequence_length: int,
) -> Path:
    out_dir = _pack_export_root(args.pack) / args.version
    out_dir.mkdir(parents=True, exist_ok=True)

    torch.save(model.state_dict(), out_dir / "model.pt")
    _export_onnx(model, out_dir / "model.onnx", sequence_length=sequence_length)

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
                "sequence_length": sequence_length,
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
    parser.add_argument("--data", choices=["synthetic", "wlasl"], default="synthetic")
    parser.add_argument("--epochs", type=int, default=80)
    parser.add_argument("--batch-size", type=int, default=32)
    parser.add_argument("--lr", type=float, default=1e-3)
    parser.add_argument("--patience", type=int, default=15)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument(
        "--samples-per-class",
        type=int,
        default=100,
        help="synthetic data source only",
    )
    parser.add_argument(
        "--sequence-length",
        type=int,
        default=DEFAULT_SEQUENCE_LENGTH,
    )
    parser.add_argument("--version", default="temporal_v1.0.0")
    parser.add_argument(
        "--pack",
        default="ase",
        help="Recognition-language pack code (packs/{code}/manifest.yaml).",
    )
    args = parser.parse_args()

    pack = _load_pack(args.pack)
    model_spec = next((m for m in pack.models if m.id == "temporal_v1"), None)
    if model_spec is None:
        raise SystemExit(
            f"Pack '{pack.recognition_code}' has no 'temporal_v1' model. "
            f"Declared models: {[m.id for m in pack.models]}"
        )
    vocab = model_spec.vocabulary
    num_classes = vocab.size
    print(
        f"pack: {pack.recognition_code} v{pack.version} — "
        f"temporal_v1 model, {num_classes} classes"
    )

    split = _load_data(
        source=args.data,
        num_classes=num_classes,
        samples_per_class=args.samples_per_class,
        sequence_length=args.sequence_length,
        seed=args.seed,
    )
    print(
        f"data: train={len(split.X_train)}, val={len(split.X_val)}, "
        f"test={len(split.X_test)}, T={args.sequence_length}"
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
    out_dir = _write_artifacts(
        model=model,
        metrics=metrics,
        args=args,
        vocab_signs=vocab_signs,
        sequence_length=args.sequence_length,
    )
    print(f"wrote: {out_dir.relative_to(_REPO_ROOT)}/")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
