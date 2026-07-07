#!/usr/bin/env python3
"""CLI wrapper — Kaggle ASL Alphabet download → landmark cache.

Prerequisites:
    1. Register at Kaggle, accept terms for
       https://www.kaggle.com/datasets/grassknoted/asl-alphabet
    2. Download the ZIP (~1 GB), extract somewhere on disk

Typical run:

    cd ml
    uv run --extra export python scripts/build_asl_cache.py \\
        --dataset-dir "/path/to/asl_alphabet_train/asl_alphabet_train" \\
        --workers 8

Fast smoke test (< 1 minute):

    uv run --extra export python scripts/build_asl_cache.py \\
        --dataset-dir "..." --max-per-class 50 \\
        --output packs/ase/data/cache/asl_alphabet_smoke.npz

After this succeeds, kick off training:

    uv run python -m sayva_ml.training.train_fingerspelling \\
        --data asl-alphabet
"""

from __future__ import annotations

import argparse
import multiprocessing
import sys
from pathlib import Path

_REPO_ROOT = Path(__file__).resolve().parents[2]
_ML_ROOT = _REPO_ROOT / "ml"
sys.path.insert(0, str(_ML_ROOT / "src"))

from sayva_ml.packs.data_loader import load_pack_data_module  # noqa: E402
from sayva_ml.packs.registry import DEFAULT_PACKS_ROOT  # noqa: E402
from sayva_ml.vocabulary import load_vocabulary  # noqa: E402


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--pack",
        default="ase",
        help="Pack code (default: ase). Kept for future retargeting; ASL is the only pack with this loader today.",
    )
    parser.add_argument(
        "--dataset-dir",
        required=True,
        type=Path,
        help="Path to `asl_alphabet_train/asl_alphabet_train/` — the dir with A/, B/, C/, … subdirs.",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=None,
        help="Output .npz path. Defaults to packs/{pack}/data/cache/asl_alphabet.npz",
    )
    parser.add_argument(
        "--workers",
        type=int,
        default=max(1, multiprocessing.cpu_count() - 1),
        help="Process-pool size for MediaPipe extraction. Defaults to CPUs - 1.",
    )
    parser.add_argument(
        "--max-per-class",
        type=int,
        default=None,
        help="Cap samples per class. Set low (~50) for a fast smoke run.",
    )
    parser.add_argument(
        "--min-detection-rate",
        type=float,
        default=0.5,
        help="Fail if any class MediaPipe detection rate falls below this.",
    )
    parser.add_argument(
        "--min-hand-detection-confidence",
        type=float,
        default=0.3,
        help=(
            "MediaPipe HandLandmarker's accept threshold for a candidate hand. "
            "Lower catches more hands (with more false positives); higher is stricter. "
            "Default matches the Android runtime for parity. Recorded in stats.json."
        ),
    )
    parser.add_argument("--train-ratio", type=float, default=0.75)
    parser.add_argument("--val-ratio", type=float, default=0.125)
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()

    pack_root = DEFAULT_PACKS_ROOT / args.pack
    if not pack_root.exists():
        raise SystemExit(f"Pack '{args.pack}' not found at {pack_root}")

    asl = load_pack_data_module(pack_root, "asl_alphabet")
    output = args.output or pack_root / "data" / "cache" / "asl_alphabet.npz"

    vocab = load_vocabulary(pack_root / "vocabularies" / "fingerspelling.yaml")

    print(f"pack:         {args.pack}")
    print(f"dataset-dir:  {args.dataset_dir}")
    print(f"vocabulary:   {vocab.size} signs ({sorted(s.id for s in vocab.signs)})")
    print(f"output:       {output}")
    print(f"workers:      {args.workers}")
    print(f"min-hand-conf: {args.min_hand_detection_confidence}")
    if args.max_per_class:
        print(f"max-per-class: {args.max_per_class}")
    print()

    split = asl.build_cache(
        dataset_dir=args.dataset_dir,
        vocabulary=vocab,
        cache_path=output,
        workers=args.workers,
        train_ratio=args.train_ratio,
        val_ratio=args.val_ratio,
        max_per_class=args.max_per_class,
        min_detection_rate=args.min_detection_rate,
        min_hand_detection_confidence=args.min_hand_detection_confidence,
        seed=args.seed,
    )
    total = len(split.X_train) + len(split.X_val) + len(split.X_test)
    print()
    print(f"wrote: {output.relative_to(_REPO_ROOT)}")
    print(f"       {output.with_suffix(output.suffix + '.meta.json').relative_to(_REPO_ROOT)}")
    print()
    print(f"totals: {total} samples "
          f"({len(split.X_train)} train / {len(split.X_val)} val / {len(split.X_test)} test)")
    print()
    print("per-class detection counts:")
    for letter, count in sorted(split.per_class_counts.items()):
        print(f"  {letter}: {count}")
    print()
    print("next: uv run python -m sayva_ml.training.train_fingerspelling --data asl-alphabet")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
