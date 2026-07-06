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
    is_valid_semver,
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


@pytest.mark.parametrize(
    "value",
    ["0.1.0", "1.0.0", "12.34.56", "1.2.3-alpha", "0.2.3-beta.1", "1.0.0+build.7"],
)
def test_semver_accepts_valid_values(value: str) -> None:
    assert is_valid_semver(value), f"expected {value!r} to be valid SemVer"


@pytest.mark.parametrize(
    "value",
    ["", "1", "1.0", "1.2.3.4", "v1.0.0", "1.0.0-", "01.2.3", "1.2.3-beta..1"],
)
def test_semver_rejects_invalid_values(value: str) -> None:
    assert not is_valid_semver(value), f"expected {value!r} to be invalid SemVer"


@pytest.mark.parametrize("pack", _all_packs(), ids=lambda p: p.recognition_code)
def test_every_bundled_pack_ships_valid_semver(pack) -> None:
    # Guards against a manifest.yaml being hand-edited to a bad version and
    # somehow bypassing the parse-time check (e.g. someone patching the
    # generator to skip validation).
    assert is_valid_semver(pack.version), (
        f"Pack '{pack.recognition_code}' has invalid version {pack.version!r}."
    )


def test_load_manifest_rejects_empty_version(tmp_path) -> None:
    _write_pack_with_version(tmp_path, version="")
    with pytest.raises(ValueError, match="non-empty SemVer"):
        load_manifest(tmp_path / "broken")


def test_load_manifest_rejects_invalid_semver(tmp_path) -> None:
    _write_pack_with_version(tmp_path, version="1.0")
    with pytest.raises(ValueError, match="not valid SemVer"):
        load_manifest(tmp_path / "broken")


def _write_pack_with_version(tmp_path, version: str) -> None:
    """Clone the first real pack into tmp_path and rewrite its version.

    Cheaper than authoring an entire manifest inline: we get every other
    required field for free and only mutate what the test cares about.
    """
    import shutil
    src = discover_packs().packs[0].pack_root
    dst = tmp_path / "broken"
    shutil.copytree(src, dst)
    manifest_path = dst / "manifest.yaml"
    text = manifest_path.read_text(encoding="utf-8")
    # Every canonical pack manifest declares `version: X.Y.Z` on its own line;
    # a targeted substitution keeps the fixture minimal.
    import re
    new_text = re.sub(
        r"^version:.*$",
        f"version: {version}" if version else "version: \"\"",
        text,
        count=1,
        flags=re.MULTILINE,
    )
    manifest_path.write_text(new_text, encoding="utf-8")


@pytest.mark.parametrize("pack", _all_packs(), ids=lambda p: p.recognition_code)
def test_distributed_manifest_carries_valid_integrity_metadata(pack) -> None:
    """Every distributed model entry must ship a SHA-256 + sizeBytes that
    match the on-disk bytes exactly.

    Guards against a regeneration bug that silently emits a stale hash
    (e.g. computing before the copy step, or against a cached file). If
    this drifts, OTA integrity verification would start rejecting valid
    packs — better to catch it in the pack pipeline than in production.
    """
    import hashlib
    import json

    _COMPOSE_ROOT = _repo_root() / "shared" / "src" / "commonMain" / "composeResources" / "files" / "language_packs"
    dist_manifest = _COMPOSE_ROOT / pack.recognition_code / "manifest.json"
    if not dist_manifest.exists():
        pytest.skip(f"Pack '{pack.recognition_code}' not distributed yet")

    payload = json.loads(dist_manifest.read_text(encoding="utf-8"))
    for entry in payload["models"]:
        integrity = entry.get("integrity")
        assert integrity, (
            f"Pack '{pack.recognition_code}' model '{entry['id']}' missing "
            f"integrity block. Run `uv run python scripts/generate_pack.py`."
        )
        expected_sha = integrity["sha256"]
        expected_size = integrity["sizeBytes"]
        assert isinstance(expected_sha, str) and len(expected_sha) == 64
        assert isinstance(expected_size, int) and expected_size > 0

        # Verify against the actual distributed file.
        model_file = _COMPOSE_ROOT / pack.recognition_code / entry["modelFile"]
        assert model_file.exists(), f"Missing distributed model file: {model_file}"
        actual_bytes = model_file.read_bytes()
        assert hashlib.sha256(actual_bytes).hexdigest() == expected_sha, (
            f"Pack '{pack.recognition_code}' model '{entry['id']}' SHA "
            f"mismatch. Re-run generate_pack.py."
        )
        assert len(actual_bytes) == expected_size


def _repo_root():
    from pathlib import Path
    return Path(__file__).resolve().parents[2]


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
