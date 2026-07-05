"""Shape/invariant checks for the synthetic temporal dataset."""

from __future__ import annotations

import numpy as np

from sayva_ml.data.synthetic_temporal import make_split


def test_split_shapes() -> None:
    split = make_split(num_classes=5, samples_per_class=20, sequence_length=30, seed=1)
    total = 5 * 20
    assert len(split.X_train) + len(split.X_val) + len(split.X_test) == total
    for X in (split.X_train, split.X_val, split.X_test):
        assert X.ndim == 3
        assert X.shape[1] == 30
        assert X.shape[2] == 84


def test_labels_cover_all_classes() -> None:
    split = make_split(num_classes=5, samples_per_class=40, seed=1)
    seen = set(split.y_train.tolist()) | set(split.y_val.tolist()) | set(split.y_test.tolist())
    assert seen == {0, 1, 2, 3, 4}


def test_features_have_reasonable_magnitude() -> None:
    """Preprocessed frames are normalized per-hand — most values should sit
    in [-1, 1]. Zero frames (pad or missing hand) are allowed."""
    split = make_split(num_classes=5, samples_per_class=10, seed=1)
    X = split.X_train
    assert X.max() <= 1.0 + 1e-5
    assert X.min() >= -1.0 - 1e-5


def test_determinism() -> None:
    a = make_split(num_classes=5, samples_per_class=10, seed=3)
    b = make_split(num_classes=5, samples_per_class=10, seed=3)
    assert np.array_equal(a.X_train, b.X_train)
    assert np.array_equal(a.y_train, b.y_train)
