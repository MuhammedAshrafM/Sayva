"""Landmark preprocessing parity — Python-side check.

Reads the shared fixture and verifies the algorithm still produces the recorded
`expected` output. Regenerate the fixture with:

    cd ml && uv run python scripts/generate_landmark_fixture.py

The corresponding Kotlin test is `LandmarkPreprocessorParityTest` in
`shared/src/androidHostTest/kotlin/org/moashraf/sayva/ml/`.
"""

from __future__ import annotations

import json
import math
from pathlib import Path

import pytest

from sayva_ml.preprocessing.landmark import (
    pre_process_landmark,
    pre_process_two_hand_frame,
    pre_process_two_hand_sequence,
)

_FIXTURE_PATH = Path(__file__).parent / "fixtures" / "landmark_parity.json"


def _load_fixture() -> dict:
    with _FIXTURE_PATH.open("r", encoding="utf-8") as f:
        return json.load(f)


def test_fixture_exists() -> None:
    assert _FIXTURE_PATH.exists(), (
        f"Regenerate with `uv run python scripts/generate_landmark_fixture.py` — "
        f"{_FIXTURE_PATH} missing."
    )


def _fixture_cases():
    return _load_fixture()["cases"]


@pytest.mark.parametrize("case", _fixture_cases(), ids=lambda c: c["id"])
def test_preprocessing_matches_fixture(case: dict) -> None:
    tol = _load_fixture()["tolerance"]
    actual = pre_process_landmark(case["raw"])
    expected = case["expected"]
    assert len(actual) == len(expected) == 42
    for i, (a, e) in enumerate(zip(actual, expected, strict=True)):
        assert math.isclose(a, e, abs_tol=tol), (
            f"case '{case['id']}' element {i}: actual={a} expected={e} tol={tol}"
        )


def test_kazuhito_case_has_unit_max_abs() -> None:
    """Sanity — Kazuhito's algorithm normalizes to max |x| == 1."""
    cases = _fixture_cases()
    kaz = next(c for c in cases if c["id"] == "kazuhito_open_hand_scaled")
    max_abs = max(abs(v) for v in pre_process_landmark(kaz["raw"]))
    assert math.isclose(max_abs, 1.0, abs_tol=1e-9)


def test_degenerate_case_returns_zeros() -> None:
    cases = _fixture_cases()
    deg = next(c for c in cases if c["id"] == "degenerate_zero_extent")
    result = pre_process_landmark(deg["raw"])
    assert all(v == 0.0 for v in result)


def test_rejects_wrong_landmark_count() -> None:
    with pytest.raises(ValueError, match="21 landmarks"):
        pre_process_landmark([[0.0, 0.0]] * 20)


@pytest.mark.parametrize(
    "case", _load_fixture()["two_hand_cases"], ids=lambda c: c["id"]
)
def test_two_hand_preprocessing_matches_fixture(case: dict) -> None:
    tol = _load_fixture()["tolerance"]
    actual = pre_process_two_hand_frame(case["left"], case["right"])
    expected = case["expected"]
    assert len(actual) == len(expected) == 84
    for i, (a, e) in enumerate(zip(actual, expected, strict=True)):
        assert math.isclose(a, e, abs_tol=tol), (
            f"case '{case['id']}' element {i}: actual={a} expected={e} tol={tol}"
        )


@pytest.mark.parametrize(
    "case", _load_fixture()["sequence_cases"], ids=lambda c: c["id"]
)
def test_sequence_preprocessing_matches_fixture(case: dict) -> None:
    tol = _load_fixture()["tolerance"]
    frames = [(f["left"], f["right"]) for f in case["frames"]]
    actual = pre_process_two_hand_sequence(frames, case["target_length"])
    expected = case["expected"]
    assert len(actual) == case["target_length"]
    assert len(actual) == len(expected)
    for row_i, (actual_row, expected_row) in enumerate(zip(actual, expected, strict=True)):
        assert len(actual_row) == 84
        for col_i, (a, e) in enumerate(zip(actual_row, expected_row, strict=True)):
            assert math.isclose(a, e, abs_tol=tol), (
                f"case '{case['id']}' [{row_i}][{col_i}]: actual={a} expected={e}"
            )


def test_two_hand_frame_missing_hand_returns_zeros() -> None:
    both_missing = pre_process_two_hand_frame(None, None)
    assert both_missing == [0.0] * 84
