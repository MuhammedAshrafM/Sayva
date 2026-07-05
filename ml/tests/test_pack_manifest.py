"""Contract tests for the Language Pack manifest schema.

Every pack in `ml/packs/*` is walked and validated. Same tests run for every
pack, so adding a new language means adding a `packs/{code}/` directory that
passes this suite — no new tests needed.
"""

from __future__ import annotations

import pytest

from sayva_ml.packs.manifest import (
    CANONICAL_OUTPUTS,
    OutputLanguageStatus,
    load_manifest,
    validate_pack,
)
from sayva_ml.packs.registry import discover_packs


def _all_packs():
    return discover_packs().packs


def test_at_least_one_pack_present() -> None:
    packs = _all_packs()
    assert packs, "Expected at least one pack under ml/packs/"


@pytest.mark.parametrize("pack", _all_packs(), ids=lambda p: p.recognition_code)
def test_pack_declares_all_canonical_outputs(pack) -> None:
    for canonical in CANONICAL_OUTPUTS:
        assert canonical in pack.supported_outputs, (
            f"Pack '{pack.recognition_code}' missing canonical output '{canonical}' "
            f"— every pack must ship labels/{canonical}.json (may be a stub)."
        )


@pytest.mark.parametrize("pack", _all_packs(), ids=lambda p: p.recognition_code)
def test_pack_output_status_covers_every_supported_output(pack) -> None:
    for out_code in pack.supported_outputs:
        assert out_code in pack.output_language_status, (
            f"Pack '{pack.recognition_code}' declares supportedOutputs={pack.supported_outputs} "
            f"but outputLanguageStatus is missing '{out_code}'."
        )
        assert isinstance(pack.output_language_status[out_code], OutputLanguageStatus)


@pytest.mark.parametrize("pack", _all_packs(), ids=lambda p: p.recognition_code)
def test_pack_default_output_is_supported(pack) -> None:
    assert pack.default_output_language in pack.supported_outputs


@pytest.mark.parametrize("pack", _all_packs(), ids=lambda p: p.recognition_code)
def test_pack_display_name_covers_every_supported_output(pack) -> None:
    for out_code in pack.supported_outputs:
        assert out_code in pack.display_name, (
            f"Pack '{pack.recognition_code}' missing displayName['{out_code}']"
        )


@pytest.mark.parametrize("pack", _all_packs(), ids=lambda p: p.recognition_code)
def test_pack_tts_locale_covers_every_supported_output(pack) -> None:
    for out_code in pack.supported_outputs:
        assert out_code in pack.tts_locale_by_output, (
            f"Pack '{pack.recognition_code}' missing ttsLocaleByOutput['{out_code}']"
        )


@pytest.mark.parametrize("pack", _all_packs(), ids=lambda p: p.recognition_code)
def test_pack_output_labels_cover_every_model_and_sign(pack) -> None:
    for out_code in pack.supported_outputs:
        assert out_code in pack.output_labels
        per_model = pack.output_labels[out_code]
        for model in pack.models:
            assert model.id in per_model, (
                f"Pack '{pack.recognition_code}' labels/{out_code}.json missing model '{model.id}'"
            )
            entries = per_model[model.id]
            vocab_ids = {s.id for s in model.vocabulary.signs}
            assert vocab_ids == set(entries.keys()), (
                f"Pack '{pack.recognition_code}' labels/{out_code}.json for model "
                f"'{model.id}' does not exactly cover vocabulary ids."
            )


@pytest.mark.parametrize("pack", _all_packs(), ids=lambda p: p.recognition_code)
def test_pack_model_output_shape_matches_vocabulary(pack) -> None:
    for model in pack.models:
        n_classes = model.output.shape[-1]
        assert model.vocabulary.size == n_classes, (
            f"Pack '{pack.recognition_code}' model '{model.id}' output shape "
            f"has {n_classes} classes but vocabulary has {model.vocabulary.size} signs."
        )


@pytest.mark.parametrize("pack", _all_packs(), ids=lambda p: p.recognition_code)
def test_validate_pack_walks_model_files(pack) -> None:
    """`validate_pack` fails loud if any referenced model file is missing on disk."""
    result = validate_pack(pack.pack_root)
    assert result.recognition_code == pack.recognition_code


def test_complete_status_implies_no_null_labels() -> None:
    """A `complete` output must have no null label entries — that's what
    'complete' means. `partial` and `stub` are allowed to have nulls."""
    for pack in _all_packs():
        for out_code, status in pack.output_language_status.items():
            if status != OutputLanguageStatus.COMPLETE:
                continue
            per_model = pack.output_labels[out_code]
            for model_id, entries in per_model.items():
                nulls = [k for k, v in entries.items() if v is None]
                assert not nulls, (
                    f"Pack '{pack.recognition_code}' labels/{out_code}.json model "
                    f"'{model_id}' is marked complete but has null entries: {nulls}"
                )


def test_missing_labels_file_fails_manifest_load(tmp_path) -> None:
    """Uniform contract check: a pack without a canonical labels file
    doesn't load."""
    from pathlib import Path
    import shutil

    src = discover_packs().packs[0].pack_root
    dst: Path = tmp_path / "broken_pack"
    shutil.copytree(src, dst)
    # Remove the ar.json file — pack should fail to load.
    (dst / "labels" / "ar.json").unlink()
    from sayva_ml.packs.manifest import load_manifest as _load

    with pytest.raises(FileNotFoundError, match="ar.json"):
        _load(dst)
