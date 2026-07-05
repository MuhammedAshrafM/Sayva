"""Basic shape/invariant checks for the synthetic fingerspelling dataset."""

from __future__ import annotations

import numpy as np

from sayva_ml.data.synthetic import make_split


def test_split_shapes() -> None:
    split = make_split(num_classes=24, samples_per_class=100, seed=42)
    total = 24 * 100
    assert (
        len(split.X_train) + len(split.X_val) + len(split.X_test) == total
    ), "Every sample must land in exactly one partition"
    assert split.X_train.shape[1] == 42
    assert split.X_val.shape[1] == 42
    assert split.X_test.shape[1] == 42


def test_labels_cover_all_classes() -> None:
    split = make_split(num_classes=24, samples_per_class=100, seed=42)
    seen = set(split.y_train.tolist()) | set(split.y_val.tolist()) | set(split.y_test.tolist())
    assert seen == set(range(24)), f"Missing classes: {set(range(24)) - seen}"


def test_features_normalized_by_preprocessing() -> None:
    """Every sample should have max-abs component ~1 (Kazuhito normalization)."""
    split = make_split(num_classes=24, samples_per_class=50, seed=42)
    for X in (split.X_train, split.X_val, split.X_test):
        max_abs_per_row = np.max(np.abs(X), axis=1)
        # Allow a tiny fp tolerance
        assert (max_abs_per_row >= 0.999).all(), max_abs_per_row.min()
        assert (max_abs_per_row <= 1.001).all(), max_abs_per_row.max()


def test_split_is_deterministic() -> None:
    a = make_split(num_classes=5, samples_per_class=10, seed=7)
    b = make_split(num_classes=5, samples_per_class=10, seed=7)
    assert np.array_equal(a.X_train, b.X_train)
    assert np.array_equal(a.y_train, b.y_train)
