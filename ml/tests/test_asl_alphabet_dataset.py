"""Tests for the ASL Alphabet dataset loader.

Verifies stratification + fail-loud behavior WITHOUT actually running
MediaPipe on real images (that would need the Kaggle download, which
belongs in a manual run, not CI). We test the loader's non-IO pieces by
constructing synthetic per-class vector sets directly.
"""

from __future__ import annotations

import importlib.util
from pathlib import Path

import numpy as np
import pytest


_ASL_CACHE = None


def _asl_module():
    """Load `packs/ase/data/asl_alphabet.py` dynamically — packs aren't a
    Python package, so we can't `import` it normally.

    Registers the module in `sys.modules` before exec — required so that
    `@dataclass` inside the module can look up its own module namespace
    during class construction."""
    import sys

    global _ASL_CACHE
    if _ASL_CACHE is not None:
        return _ASL_CACHE

    repo_root = Path(__file__).resolve().parents[2]
    path = repo_root / "ml" / "packs" / "ase" / "data" / "asl_alphabet.py"
    module_name = "_test_asl_module"
    spec = importlib.util.spec_from_file_location(module_name, path)
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    sys.modules[module_name] = module
    spec.loader.exec_module(module)
    _ASL_CACHE = module
    return module


def test_stratified_split_covers_every_class_in_every_partition() -> None:
    asl = _asl_module()
    # 5 classes × 40 samples each. Every partition should see all 5 classes.
    labels = [c for c in range(5) for _ in range(40)]
    train, val, test = asl._stratified_indices(labels, train_ratio=0.75, val_ratio=0.125, seed=1)
    for partition in (train, val, test):
        seen = {labels[i] for i in partition}
        assert seen == {0, 1, 2, 3, 4}, f"partition missing classes: {partition[:5]}"


def test_stratified_split_ratios_are_within_one_sample_per_class() -> None:
    asl = _asl_module()
    labels = [c for c in range(4) for _ in range(100)]
    train, val, test = asl._stratified_indices(labels, train_ratio=0.75, val_ratio=0.125, seed=42)
    # Every class should contribute 75/12/13 (train/val/test) within ±1.
    for c in range(4):
        train_count = sum(1 for i in train if labels[i] == c)
        val_count = sum(1 for i in val if labels[i] == c)
        test_count = sum(1 for i in test if labels[i] == c)
        assert 74 <= train_count <= 76
        assert 11 <= val_count <= 13
        assert 11 <= test_count <= 14
        assert train_count + val_count + test_count == 100


def test_stratified_split_deterministic_with_same_seed() -> None:
    asl = _asl_module()
    labels = [c % 3 for c in range(60)]
    a = asl._stratified_indices(labels, 0.75, 0.125, seed=99)
    b = asl._stratified_indices(labels, 0.75, 0.125, seed=99)
    assert a == b


def test_load_cache_returns_arrays(tmp_path: Path) -> None:
    asl = _asl_module()
    cache = tmp_path / "smoke.npz"
    np.savez(
        cache,
        X_train=np.zeros((3, 42), dtype=np.float32),
        y_train=np.array([0, 1, 2], dtype=np.int64),
        X_val=np.zeros((1, 42), dtype=np.float32),
        y_val=np.array([0], dtype=np.int64),
        X_test=np.zeros((1, 42), dtype=np.float32),
        y_test=np.array([1], dtype=np.int64),
    )
    split = asl.load_cache(cache)
    assert split.X_train.shape == (3, 42)
    assert split.per_class_counts == {}  # no sidecar → empty


def test_load_cache_reads_sidecar_counts(tmp_path: Path) -> None:
    import json

    asl = _asl_module()
    cache = tmp_path / "with_meta.npz"
    np.savez(
        cache,
        X_train=np.zeros((1, 42), dtype=np.float32),
        y_train=np.array([0], dtype=np.int64),
        X_val=np.zeros((1, 42), dtype=np.float32),
        y_val=np.array([0], dtype=np.int64),
        X_test=np.zeros((1, 42), dtype=np.float32),
        y_test=np.array([0], dtype=np.int64),
    )
    meta = cache.with_suffix(cache.suffix + ".meta.json")
    meta.write_text(
        json.dumps({"per_class_counts": {"A": 500, "B": 420}}),
        encoding="utf-8",
    )
    split = asl.load_cache(cache)
    assert split.per_class_counts == {"A": 500, "B": 420}


def test_build_cache_raises_when_dataset_dir_missing(tmp_path: Path) -> None:
    asl = _asl_module()
    from sayva_ml.vocabulary import load_vocabulary

    from sayva_ml.packs.registry import DEFAULT_PACKS_ROOT
    vocab = load_vocabulary(DEFAULT_PACKS_ROOT / "ase" / "vocabularies" / "fingerspelling.yaml")
    with pytest.raises(FileNotFoundError, match="ASL Alphabet dataset not found"):
        asl.build_cache(
            dataset_dir=tmp_path / "does-not-exist",
            vocabulary=vocab,
            cache_path=tmp_path / "unused.npz",
        )
