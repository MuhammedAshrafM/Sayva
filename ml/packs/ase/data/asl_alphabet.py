"""Real ASL Alphabet Dataset loader — Kaggle → landmark cache.

Dataset: https://www.kaggle.com/datasets/grassknoted/asl-alphabet
    - 87,000 200×200 RGB training images across 29 classes
    - We keep only the 24 letters in `packs/ase/vocabularies/fingerspelling.yaml`
      (i.e. skip J, Z, `del`, `nothing`, `space`)

**Not runnable inside a fresh session** — the dataset must be downloaded
manually first (Kaggle credentials required). Once the archive is extracted,
run:

    cd ml
    uv run --extra export python -m scripts.build_asl_cache \\
        --dataset-dir packs/ase/data/downloads/asl_alphabet_train/asl_alphabet_train \\
        --output packs/ase/data/cache/asl_alphabet.npz \\
        --workers 8

The cache is what `train_fingerspelling.py --data asl-alphabet` reads.

## Design notes

Single-process MediaPipe on 87k images runs 30–60 minutes. Multiprocess with
`ProcessPoolExecutor` cuts that to ~8 minutes on 8 cores. Each worker
initializes its own MediaPipe `Hands` instance once and reuses it — hand
detector construction is the dominant per-call cost, not the actual model.

Split is **stratified**: every class contributes the same proportion of
samples to train/val/test partitions. Prevents lopsided evaluation when
some letters have fewer successful MediaPipe detections than others.

Metadata sidecar (`.meta.json`) records:
    * Per-class raw image counts + successful detection counts
    * MediaPipe detection rate per class (low rates suggest bad training data)
    * Total wall-clock time
    * `pre_process_landmark` version reference for reproducibility

If detection rate for any class falls below `min_detection_rate`, the build
fails loud. This is the safety rail — better to fix training data now than
train on 5% of Y-samples and wonder why Y accuracy is bad.
"""

from __future__ import annotations

import json
import time
from concurrent.futures import ProcessPoolExecutor, as_completed
from dataclasses import asdict, dataclass, field
from pathlib import Path

import numpy as np

from sayva_ml.data.mediapipe_workers import extract_one, init_worker
from sayva_ml.preprocessing.landmark import pre_process_landmark  # noqa: F401 — kept for backward-compat imports
from sayva_ml.vocabulary import Vocabulary

# Sentinel — updated by the caller when the dataset lands.
DEFAULT_DATASET_DIR = Path("packs/ase/data/downloads/asl_alphabet_train/asl_alphabet_train")

MIN_CLASS_DETECTION_RATE = 0.5  # fail-loud floor per letter (50% of images produce a detected hand)


@dataclass(frozen=True)
class RealDatasetSplit:
    X_train: np.ndarray
    y_train: np.ndarray
    X_val: np.ndarray
    y_val: np.ndarray
    X_test: np.ndarray
    y_test: np.ndarray
    per_class_counts: dict[str, int]


@dataclass
class ClassStats:
    letter: str
    images_seen: int = 0
    images_with_hand: int = 0

    @property
    def detection_rate(self) -> float:
        return self.images_with_hand / self.images_seen if self.images_seen else 0.0


@dataclass
class BuildStats:
    per_class: dict[str, ClassStats] = field(default_factory=dict)
    total_seconds: float = 0.0
    workers: int = 1
    preprocessor_version: str = "kazuhito_v1"

    def to_json_dict(self) -> dict:
        return {
            "per_class": {
                letter: {
                    "images_seen": s.images_seen,
                    "images_with_hand": s.images_with_hand,
                    "detection_rate": round(s.detection_rate, 4),
                }
                for letter, s in self.per_class.items()
            },
            "total_seconds": round(self.total_seconds, 2),
            "workers": self.workers,
            "preprocessor_version": self.preprocessor_version,
        }


# ---------------------------------------------------------------------------
# Worker path
# ---------------------------------------------------------------------------
#
# Extraction workers live in `sayva_ml.data.mediapipe_workers` — a
# language-neutral module in the installed `sayva_ml` package — because
# `ProcessPoolExecutor.spawn` (Windows default) requires worker callables
# to be reachable via a real Python import path in the child process. This
# pack file is loaded dynamically by
# `sayva_ml.packs.data_loader.load_pack_data_module`, so its
# `_pack_ase_asl_alphabet` qualname exists only in the parent's sys.modules
# and cannot be resolved by spawn'd children. Keeping the workers next
# door in `sayva_ml/data/` sidesteps that entirely and stays available to
# every future pack that wants to run the same MediaPipe-on-images
# pipeline over its own dataset.


# ---------------------------------------------------------------------------
# Public API
# ---------------------------------------------------------------------------

def _stratified_indices(
    labels: list[int],
    train_ratio: float,
    val_ratio: float,
    seed: int,
) -> tuple[list[int], list[int], list[int]]:
    """Stratified train/val/test partition — each class contributes the same
    proportion to each partition. Deterministic per `seed`."""
    import random

    rng = random.Random(seed)
    per_class: dict[int, list[int]] = {}
    for i, y in enumerate(labels):
        per_class.setdefault(y, []).append(i)

    train_idx: list[int] = []
    val_idx: list[int] = []
    test_idx: list[int] = []
    for class_indices in per_class.values():
        rng.shuffle(class_indices)
        n = len(class_indices)
        n_train = int(n * train_ratio)
        n_val = int(n * val_ratio)
        train_idx.extend(class_indices[:n_train])
        val_idx.extend(class_indices[n_train : n_train + n_val])
        test_idx.extend(class_indices[n_train + n_val :])

    rng.shuffle(train_idx)
    rng.shuffle(val_idx)
    rng.shuffle(test_idx)
    return train_idx, val_idx, test_idx


def build_cache(  # noqa: PLR0913
    dataset_dir: Path,
    vocabulary: Vocabulary,
    cache_path: Path,
    workers: int = 4,
    train_ratio: float = 0.75,
    val_ratio: float = 0.125,
    max_per_class: int | None = None,
    min_detection_rate: float = MIN_CLASS_DETECTION_RATE,
    min_hand_detection_confidence: float = 0.3,
    seed: int = 42,
    progress: bool = True,
) -> RealDatasetSplit:
    """Walk `dataset_dir`, run MediaPipe in parallel, split, and cache.

    Args:
        dataset_dir: Path to `asl_alphabet_train/asl_alphabet_train/` — the
            directory that directly contains one subdir per letter (A/, B/, …).
        vocabulary: Loaded fingerspelling vocab; the loader only visits
            subdirectories whose names match a vocab sign ID.
        cache_path: Output `.npz` path. Sidecar `.meta.json` written next to it.
        workers: Process-pool size. Set to 1 to debug; use CPU-count otherwise.
        max_per_class: Sample cap per class — useful for quick smoke runs
            (`--max-per-class 100` finishes in seconds). `None` = use every image.
        min_detection_rate: Reject the build if any class falls below this
            per-class MediaPipe detection rate. Guards against silently
            training on lopsided data.
        min_hand_detection_confidence: MediaPipe's own accept threshold for
            a candidate hand. Lower catches more hands (including some
            false positives); higher is stricter. Recorded in the stats.json
            so downstream analyses know which detector config produced
            which cache.
        seed: RNG seed for shuffling within stratified partitions.
        progress: Print a per-1000-image progress line.

    Raises:
        FileNotFoundError: `dataset_dir` missing.
        RuntimeError: any class falls below `min_detection_rate`.
    """
    if not dataset_dir.exists():
        raise FileNotFoundError(
            f"ASL Alphabet dataset not found at {dataset_dir}. "
            f"Download from Kaggle: https://www.kaggle.com/datasets/grassknoted/asl-alphabet"
        )

    vocab_ids = {s.id for s in vocabulary.signs}
    image_paths: list[str] = []
    for letter_dir in sorted(dataset_dir.iterdir()):
        if not letter_dir.is_dir() or letter_dir.name not in vocab_ids:
            continue
        letter_images = sorted(letter_dir.glob("*.jpg"))
        if max_per_class is not None:
            letter_images = letter_images[:max_per_class]
        image_paths.extend(str(p) for p in letter_images)

    if not image_paths:
        raise RuntimeError(
            f"No JPGs matched vocab under {dataset_dir}. "
            f"Expected one subdir per letter (A/, B/, …)."
        )

    stats = BuildStats(workers=workers)
    for sign_id in vocab_ids:
        stats.per_class[sign_id] = ClassStats(letter=sign_id)

    per_class_vectors: dict[str, list[list[float]]] = {sid: [] for sid in vocab_ids}

    start = time.perf_counter()
    completed = 0
    with ProcessPoolExecutor(
        max_workers=workers,
        initializer=init_worker,
        initargs=(None, min_hand_detection_confidence),
    ) as pool:
        futures = [pool.submit(extract_one, p) for p in image_paths]
        for fut in as_completed(futures):
            letter, vec = fut.result()
            s = stats.per_class[letter]
            s.images_seen += 1
            if vec is not None:
                s.images_with_hand += 1
                per_class_vectors[letter].append(vec)
            completed += 1
            if progress and completed % 1000 == 0:
                elapsed = time.perf_counter() - start
                pct = completed / len(image_paths) * 100
                print(f"[{completed:>6d}/{len(image_paths)}] {pct:5.1f}% · {elapsed:5.1f}s")

    stats.total_seconds = time.perf_counter() - start

    # Always dump per-class stats, even when the QA gate below raises.
    # A failing run's per-class rates are exactly what a controlled
    # detector-config comparison needs — waiting for the gate to pass
    # to see them would mean throwing away 17+ minutes of extraction.
    stats_path = cache_path.with_suffix(cache_path.suffix + ".stats.json")
    stats_path.parent.mkdir(parents=True, exist_ok=True)
    stats_path.write_text(
        json.dumps(
            {
                "dataset_dir": str(dataset_dir),
                "min_detection_rate_threshold": min_detection_rate,
                "min_hand_detection_confidence": min_hand_detection_confidence,
                "build": stats.to_json_dict(),
            },
            indent=2,
        ),
        encoding="utf-8",
    )

    # Quality gate — refuse to write a lopsided dataset.
    weak_classes = [
        (letter, s)
        for letter, s in stats.per_class.items()
        if s.images_seen > 0 and s.detection_rate < min_detection_rate
    ]
    if weak_classes:
        details = ", ".join(
            f"{letter}({s.detection_rate * 100:.1f}%)" for letter, s in weak_classes
        )
        raise RuntimeError(
            f"Detection rate below {min_detection_rate * 100:.0f}% for: {details}. "
            "Training on this data would produce a biased model. "
            f"Full per-class stats at {stats_path}. "
            "Either widen `min_detection_rate` (with eyes open) or collect more data."
        )

    # Assemble arrays + stratified split.
    all_X: list[list[float]] = []
    all_y: list[int] = []
    per_class_counts: dict[str, int] = {}
    for letter, vectors in per_class_vectors.items():
        idx = vocabulary.index_of(letter)
        per_class_counts[letter] = len(vectors)
        for v in vectors:
            all_X.append(v)
            all_y.append(idx)

    train_idx, val_idx, test_idx = _stratified_indices(all_y, train_ratio, val_ratio, seed)
    X = np.asarray(all_X, dtype=np.float32)
    y = np.asarray(all_y, dtype=np.int64)
    split = RealDatasetSplit(
        X_train=X[train_idx], y_train=y[train_idx],
        X_val=X[val_idx], y_val=y[val_idx],
        X_test=X[test_idx], y_test=y[test_idx],
        per_class_counts=per_class_counts,
    )

    cache_path.parent.mkdir(parents=True, exist_ok=True)
    np.savez(
        cache_path,
        X_train=split.X_train, y_train=split.y_train,
        X_val=split.X_val, y_val=split.y_val,
        X_test=split.X_test, y_test=split.y_test,
    )
    meta_path = cache_path.with_suffix(cache_path.suffix + ".meta.json")
    meta_path.write_text(
        json.dumps({
            "dataset_dir": str(dataset_dir),
            "cache_path": str(cache_path),
            "train_ratio": train_ratio,
            "val_ratio": val_ratio,
            "test_ratio": round(1 - train_ratio - val_ratio, 4),
            "max_per_class": max_per_class,
            "min_detection_rate": min_detection_rate,
            "seed": seed,
            "totals": {
                "images_seen": sum(s.images_seen for s in stats.per_class.values()),
                "images_with_hand": sum(s.images_with_hand for s in stats.per_class.values()),
                "train_samples": int(len(split.X_train)),
                "val_samples": int(len(split.X_val)),
                "test_samples": int(len(split.X_test)),
            },
            "per_class_counts": per_class_counts,
            "build": stats.to_json_dict(),
        }, indent=2),
        encoding="utf-8",
    )

    return split


def load_cache(cache_path: Path) -> RealDatasetSplit:
    data = np.load(cache_path)
    counts: dict[str, int] = {}
    meta_path = cache_path.with_suffix(cache_path.suffix + ".meta.json")
    if meta_path.exists():
        meta = json.loads(meta_path.read_text(encoding="utf-8"))
        counts = dict(meta.get("per_class_counts", {}))
    return RealDatasetSplit(
        X_train=data["X_train"],
        y_train=data["y_train"],
        X_val=data["X_val"],
        y_val=data["y_val"],
        X_test=data["X_test"],
        y_test=data["y_test"],
        per_class_counts=counts,
    )
