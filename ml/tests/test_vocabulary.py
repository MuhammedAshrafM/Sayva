"""Tests for the vocabulary loader — verifies the new pack-neutral schema."""

from __future__ import annotations

from pathlib import Path

import pytest
import yaml

from sayva_ml.packs.registry import DEFAULT_PACKS_ROOT
from sayva_ml.vocabulary import Vocabulary, load_vocabulary

_ASE_VOCABS = DEFAULT_PACKS_ROOT / "ase" / "vocabularies"


def test_ase_fingerspelling_loads() -> None:
    vocab = load_vocabulary(_ASE_VOCABS / "fingerspelling.yaml")
    assert isinstance(vocab, Vocabulary)
    assert vocab.size == 24, "Alphabet skips J and Z"
    ids = {s.id for s in vocab.signs}
    assert "A" in ids and "Y" in ids
    assert "J" not in ids and "Z" not in ids


def test_ase_temporal_loads() -> None:
    vocab = load_vocabulary(_ASE_VOCABS / "temporal_v1.yaml")
    assert vocab.size == 5
    assert [s.id for s in vocab.signs] == ["HELLO", "THANK_YOU", "PLEASE", "SORRY", "YES"]


def test_vocabulary_ids_are_unique() -> None:
    for vocab_file in _ASE_VOCABS.glob("*.yaml"):
        vocab = load_vocabulary(vocab_file)
        ids = [s.id for s in vocab.signs]
        assert len(ids) == len(set(ids)), f"Duplicate ids in {vocab_file}"


def test_index_of_matches_declared_order() -> None:
    vocab = load_vocabulary(_ASE_VOCABS / "temporal_v1.yaml")
    for expected_idx, sign in enumerate(vocab.signs):
        assert vocab.index_of(sign.id) == expected_idx


def test_index_of_unknown_raises() -> None:
    vocab = load_vocabulary(_ASE_VOCABS / "temporal_v1.yaml")
    with pytest.raises(KeyError):
        vocab.index_of("definitely_not_a_real_sign_id")


def test_malformed_vocabulary_raises(tmp_path: Path) -> None:
    bad = tmp_path / "bad.yaml"
    bad.write_text(yaml.safe_dump({"version": 1}), encoding="utf-8")
    with pytest.raises(ValueError, match="signs"):
        load_vocabulary(bad)


def test_duplicate_sign_ids_rejected(tmp_path: Path) -> None:
    bad = tmp_path / "dup.yaml"
    bad.write_text(
        yaml.safe_dump(
            {
                "version": 1,
                "signs": [{"id": "HELLO"}, {"id": "HELLO"}],
            }
        ),
        encoding="utf-8",
    )
    with pytest.raises(ValueError, match="duplicate"):
        load_vocabulary(bad)


def test_new_schema_has_no_label_field() -> None:
    """Regression guard: the vocabulary loader must not accept or store labels.

    Labels moved to `labels/{outputCode}.json`. If somebody adds `label:` back
    to a vocab file expecting it to work, the loader silently ignores it —
    which is a subtle bug. This test documents the expected loss and can
    be revisited if we ever decide to reintroduce labels here."""
    tmp = Path(__file__).parent / "_tmp_label_schema.yaml"
    tmp.write_text(
        yaml.safe_dump(
            {
                "version": 1,
                "signs": [{"id": "A", "label": "should be ignored", "tags": []}],
            }
        ),
        encoding="utf-8",
    )
    try:
        vocab = load_vocabulary(tmp)
        assert vocab.size == 1
        # Sign dataclass has no `label` attribute — labels come from the pack's
        # per-output labels file, not from the vocab.
        assert not hasattr(vocab.signs[0], "label")
    finally:
        tmp.unlink(missing_ok=True)
