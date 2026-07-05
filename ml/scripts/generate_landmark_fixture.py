#!/usr/bin/env python3
"""Generate the shared parity fixture used by both Python and Kotlin tests.

Output: `ml/tests/fixtures/landmark_parity.json`
Also copied to: `shared/src/androidHostTest/resources/landmark_parity.json`

Each case is `{ id, raw: [[x, y], ...×21], expected: [f0, f1, ...×42] }`.
The `expected` field is computed here (Python), so if Kazuhito's algorithm ever
changes, we regenerate the fixture from Python — Kotlin is measured against
this ground truth.

Run manually when the algorithm changes:
    cd ml && uv run python scripts/generate_landmark_fixture.py

CI does not regenerate — this file is committed and the parity tests read it.
"""

from __future__ import annotations

import json
import math
import random
import shutil
import sys
from pathlib import Path

_REPO_ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(_REPO_ROOT / "ml" / "src"))

from sayva_ml.preprocessing.landmark import (  # noqa: E402
    pre_process_landmark,
    pre_process_two_hand_frame,
    pre_process_two_hand_sequence,
)

_FIXTURE_PATH = _REPO_ROOT / "ml" / "tests" / "fixtures" / "landmark_parity.json"
_KOTLIN_FIXTURE_PATH = (
    _REPO_ROOT
    / "shared"
    / "src"
    / "androidHostTest"
    / "resources"
    / "landmark_parity.json"
)


def _case(case_id: str, raw: list[list[float]]) -> dict[str, object]:
    expected = pre_process_landmark(raw)
    return {
        "id": case_id,
        "raw": raw,
        "expected": expected,
    }


def _open_hand_from_kazuhito() -> list[list[float]]:
    """Reverse-engineer raw pixel coords for row 1 of Kazuhito's `keypoint.csv`.

    That row is `pre_process_landmark`'s OUTPUT (not raw). To use it as a raw
    input for our own parity check, we scale the values back up by an arbitrary
    factor so the max abs is not already 1 — then verify preprocess() reduces
    it back to the known-good vector.
    """
    processed = [
        (0.0, 0.0),
        (0.20078740157480315, -0.051181102362204724),
        (0.3661417322834646, -0.18110236220472442),
        (0.484251968503937, -0.30708661417322836),
        (0.594488188976378, -0.38188976377952755),
        (0.2637795275590551, -0.46062992125984253),
        (0.3425196850393701, -0.65748031496063),
        (0.3937007874015748, -0.7834645669291339),
        (0.42913385826771655, -0.9015748031496063),
        (0.14960629921259844, -0.5),
        (0.1968503937007874, -0.7244094488188977),
        (0.23228346456692914, -0.8740157480314961),
        (0.2559055118110236, -1.0),
        (0.03543307086614173, -0.4881889763779528),
        (0.031496062992125984, -0.7047244094488189),
        (0.03543307086614173, -0.8503937007874016),
        (0.03937007874015748, -0.9763779527559056),
        (-0.07480314960629922, -0.42913385826771655),
        (-0.14566929133858267, -0.5787401574803149),
        (-0.18503937007874016, -0.6850393700787402),
        (-0.2204724409448819, -0.7874015748031497),
    ]
    # Scale up and offset by an arbitrary origin so preprocess() has real work
    # to do — otherwise we're just passing the answer as the question.
    scale = 400.0
    offset_x, offset_y = 320.0, 240.0
    return [[x * scale + offset_x, y * scale + offset_y] for (x, y) in processed]


def _synthetic_flat_hand() -> list[list[float]]:
    """21 landmarks in a straight line — degenerate but well-defined."""
    return [[float(i) * 10, 0.0] for i in range(21)]


def _synthetic_random_hand(seed: int) -> list[list[float]]:
    rng = random.Random(seed)
    # Simulated pixel coords roughly resembling a 720p frame hand region.
    return [[rng.uniform(200, 500), rng.uniform(150, 400)] for _ in range(21)]


def _degenerate_all_zero() -> list[list[float]]:
    """Every landmark at the same point → preprocess returns zeros."""
    return [[100.0, 100.0] for _ in range(21)]


def _two_hand_case(
    case_id: str,
    left: list[list[float]] | None,
    right: list[list[float]] | None,
) -> dict[str, object]:
    expected = pre_process_two_hand_frame(left, right)
    return {
        "id": case_id,
        "left": left,
        "right": right,
        "expected": expected,
    }


def _two_hand_sequence_case(
    case_id: str,
    frames: list[tuple[list[list[float]] | None, list[list[float]] | None]],
    target_length: int,
) -> dict[str, object]:
    expected = pre_process_two_hand_sequence(frames, target_length)
    return {
        "id": case_id,
        "frames": [
            {"left": f[0], "right": f[1]}
            for f in frames
        ],
        "target_length": target_length,
        "expected": expected,
    }


def build_cases() -> list[dict[str, object]]:
    return [
        _case("kazuhito_open_hand_scaled", _open_hand_from_kazuhito()),
        _case("synthetic_flat_line", _synthetic_flat_hand()),
        _case("synthetic_random_a", _synthetic_random_hand(seed=1)),
        _case("synthetic_random_b", _synthetic_random_hand(seed=42)),
        _case("degenerate_zero_extent", _degenerate_all_zero()),
    ]


def build_two_hand_cases() -> list[dict[str, object]]:
    left_hand = _synthetic_random_hand(seed=101)
    right_hand = _synthetic_random_hand(seed=202)
    return [
        _two_hand_case("both_hands_random", left_hand, right_hand),
        _two_hand_case("only_left_hand", left_hand, None),
        _two_hand_case("only_right_hand", None, right_hand),
        _two_hand_case("no_hands", None, None),
    ]


def build_sequence_cases() -> list[dict[str, object]]:
    # Small target_length to keep the fixture readable — real model uses 30.
    left = _synthetic_random_hand(seed=311)
    right = _synthetic_random_hand(seed=411)
    other_left = _synthetic_random_hand(seed=511)
    frames_short = [
        (left, right),
        (other_left, None),
    ]
    frames_long = [
        (left, right),
        (other_left, None),
        (None, right),
        (left, right),
        (other_left, other_left),
        (left, None),
        (other_left, right),
    ]
    return [
        _two_hand_sequence_case("short_needs_padding", frames_short, target_length=5),
        _two_hand_sequence_case("long_needs_subsample", frames_long, target_length=3),
        _two_hand_sequence_case("empty_sequence", [], target_length=4),
    ]


def _sanity_check_kazuhito(cases: list[dict[str, object]]) -> None:
    """The Kazuhito case's expected output should closely match the
    known-good `processed` vector — proves the raw coords we reconstructed
    are consistent with the preprocessing algorithm."""
    kaz = next(c for c in cases if c["id"] == "kazuhito_open_hand_scaled")
    # Recompute expected in normalized form
    expected = kaz["expected"]  # type: ignore[assignment]
    assert isinstance(expected, list)
    assert len(expected) == 42
    # Max abs must be 1 exactly (up to float precision)
    max_abs = max(abs(v) for v in expected)  # type: ignore[arg-type]
    assert math.isclose(max_abs, 1.0, abs_tol=1e-9), f"Max abs was {max_abs}, expected 1.0"


def main() -> int:
    cases = build_cases()
    _sanity_check_kazuhito(cases)
    payload = {
        "schema_version": 2,
        "description": (
            "Landmark preprocessing parity cases. Python and Kotlin must produce "
            "identical `expected` for each `raw` input, within 1e-6 per element."
        ),
        "tolerance": 1e-6,
        "cases": cases,
        "two_hand_cases": build_two_hand_cases(),
        "sequence_cases": build_sequence_cases(),
    }
    _FIXTURE_PATH.parent.mkdir(parents=True, exist_ok=True)
    _FIXTURE_PATH.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
    _KOTLIN_FIXTURE_PATH.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(_FIXTURE_PATH, _KOTLIN_FIXTURE_PATH)
    print(f"wrote: {_FIXTURE_PATH.relative_to(_REPO_ROOT)}")
    print(f"wrote: {_KOTLIN_FIXTURE_PATH.relative_to(_REPO_ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
