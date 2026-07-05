"""Synthetic fingerspelling dataset — one class-specific pose per letter, with
Gaussian noise around it.

Purpose: prove the training→export→consumption pipeline shape works without
downloading Kaggle's ASL Alphabet Dataset (~1 GB). Real dataset code lives in
`asl_alphabet.py`; this module produces obviously-separable clusters so the
MLP trains to near-perfect accuracy in seconds.

Not a substitute for real data — the accuracy this dataset produces has no
predictive value for real ASL images. It only proves the pipe doesn't leak.
"""

from __future__ import annotations

import random
from dataclasses import dataclass

import numpy as np

from sayva_ml.preprocessing.landmark import pre_process_landmark


@dataclass(frozen=True)
class SyntheticSplit:
    """A single train/val/test split as flat float32 arrays."""

    X_train: np.ndarray  # shape (N_train, 42)
    y_train: np.ndarray  # shape (N_train,)
    X_val: np.ndarray
    y_val: np.ndarray
    X_test: np.ndarray
    y_test: np.ndarray


def _prototype_for_class(class_index: int, seed: int) -> list[list[float]]:
    """Generate a deterministic 21-landmark 'prototype hand' for one class.

    We seed the RNG on `class_index` so every class has a different, stable
    canonical pose. Downstream noise (see `sample_around_prototype`) is what
    creates train/val/test diversity.
    """
    rng = random.Random(1000 + class_index * 7 + seed)
    return [[rng.uniform(200, 500), rng.uniform(150, 400)] for _ in range(21)]


def sample_around_prototype(
    prototype: list[list[float]],
    noise_px: float,
    rng: random.Random,
) -> list[list[float]]:
    """Perturb each landmark by Gaussian noise; return a new (21, 2) list."""
    return [
        [
            p[0] + rng.gauss(0.0, noise_px),
            p[1] + rng.gauss(0.0, noise_px),
        ]
        for p in prototype
    ]


def make_split(
    num_classes: int,
    samples_per_class: int,
    train_ratio: float = 0.75,
    val_ratio: float = 0.125,
    noise_px: float = 6.0,
    seed: int = 42,
) -> SyntheticSplit:
    """Build a full labeled dataset with a fixed train/val/test partition.

    `noise_px` is small enough that classes remain well-separated but large
    enough that the model must generalize — landing on 100% training accuracy
    but ~5% below on test.
    """
    rng = random.Random(seed)
    prototypes = [_prototype_for_class(i, seed) for i in range(num_classes)]

    all_X: list[list[float]] = []
    all_y: list[int] = []
    for class_idx, proto in enumerate(prototypes):
        for _ in range(samples_per_class):
            raw = sample_around_prototype(proto, noise_px, rng)
            processed = pre_process_landmark(raw)
            all_X.append(processed)
            all_y.append(class_idx)

    # Shuffle deterministically, then split.
    indices = list(range(len(all_X)))
    rng.shuffle(indices)
    X = np.asarray([all_X[i] for i in indices], dtype=np.float32)
    y = np.asarray([all_y[i] for i in indices], dtype=np.int64)

    n = len(X)
    n_train = int(n * train_ratio)
    n_val = int(n * val_ratio)
    return SyntheticSplit(
        X_train=X[:n_train],
        y_train=y[:n_train],
        X_val=X[n_train : n_train + n_val],
        y_val=y[n_train : n_train + n_val],
        X_test=X[n_train + n_val :],
        y_test=y[n_train + n_val :],
    )
