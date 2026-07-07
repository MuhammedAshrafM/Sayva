"""Regression test — the trained model still matches the pinned golden fixture.

Guards against silent drift in the training / preprocessing / model export
paths: if someone retrains the ASE fingerspelling model, changes
`pre_process_landmark`, or reorders the vocabulary, this test fails
loudly and the developer regenerates the fixture with
`scripts/generate_golden_inference.py`.

The Kotlin counterpart of this test lives at
`shared/src/androidHostTest/kotlin/org/moashraf/sayva/ml/GoldenInferenceTest.kt`.
Both sides consume the same JSON fixture. If they disagree on either
`features_42` (from `raw_landmarks_21`) or `logits_24` (from
`features_42`), the pipeline has a train/serve mismatch.
"""

from __future__ import annotations

import json
import math
from pathlib import Path

import numpy as np
import pytest
import torch

from sayva_ml.models.fingerspelling_mlp import FingerspellingMLP
from sayva_ml.packs.registry import DEFAULT_PACKS_ROOT
from sayva_ml.preprocessing.landmark import pre_process_landmark
from sayva_ml.vocabulary import load_vocabulary

_FIXTURE_PATH = Path(__file__).resolve().parent / "fixtures" / "golden_inference.json"


@pytest.fixture(scope="module")
def fixture() -> dict:
    if not _FIXTURE_PATH.exists():
        pytest.skip(
            f"Missing {_FIXTURE_PATH}. Regenerate with "
            f"`uv run --extra export python scripts/generate_golden_inference.py`."
        )
    return json.loads(_FIXTURE_PATH.read_text(encoding="utf-8"))


@pytest.fixture(scope="module")
def model_and_vocab(fixture) -> tuple[FingerspellingMLP, list[str]]:
    pack_root = DEFAULT_PACKS_ROOT / fixture["pack_code"]
    vocab_signs = [s.id for s in load_vocabulary(
        pack_root / "vocabularies" / "fingerspelling.yaml"
    ).signs]
    checkpoint = (
        pack_root / "models" / "exported" / fixture["model_version"] / "model.pt"
    )
    if not checkpoint.exists():
        pytest.skip(
            f"Missing {checkpoint}. Retrain the {fixture['pack_code']} pack's "
            f"{fixture['model_version']} to run golden inference."
        )
    model = FingerspellingMLP(num_classes=len(vocab_signs))
    model.load_state_dict(torch.load(checkpoint, map_location="cpu"))
    model.eval()
    return model, vocab_signs


def test_vocab_order_matches_fixture(fixture, model_and_vocab) -> None:
    """Fixture pins a vocabulary snapshot; the pack's must still match it."""
    _, vocab_signs = model_and_vocab
    assert vocab_signs == fixture["vocab_order"], (
        "Vocabulary order drifted from the golden fixture. Either the pack's "
        "`vocabularies/fingerspelling.yaml` changed order, or the fixture is "
        "stale — regenerate it if the change was intentional."
    )


@pytest.mark.parametrize("case_index", range(0, 6), ids=lambda i: f"case_{i}")
def test_features_from_raw_landmarks_match(fixture, case_index: int) -> None:
    """`pre_process_landmark(raw_landmarks_21)` must exactly reproduce
    the pinned `features_42`. Bit-for-bit within `tolerance_features`."""
    case = fixture["cases"][case_index]
    recomputed = pre_process_landmark(case["raw_landmarks_21"])
    tol = fixture["tolerance_features"]
    for i, (got, expected) in enumerate(zip(recomputed, case["features_42"])):
        assert math.isclose(got, expected, abs_tol=tol), (
            f"{case['sign_id']}: feature[{i}] drifted "
            f"{got:.9f} vs pinned {expected:.9f} (tol {tol})"
        )


@pytest.mark.parametrize("case_index", range(0, 6), ids=lambda i: f"case_{i}")
def test_logits_from_features_match(fixture, model_and_vocab, case_index: int) -> None:
    """The current model.pt fed `features_42` must reproduce `logits_24`
    within `tolerance_logits`. Guards the training / export path."""
    model, _ = model_and_vocab
    case = fixture["cases"][case_index]
    with torch.inference_mode():
        tensor = torch.tensor([case["features_42"]], dtype=torch.float32)
        recomputed = model(tensor).squeeze(0).tolist()
    tol = fixture["tolerance_logits"]
    for i, (got, expected) in enumerate(zip(recomputed, case["logits_24"])):
        assert math.isclose(got, expected, abs_tol=tol), (
            f"{case['sign_id']}: logit[{i}] drifted {got:.6f} vs pinned "
            f"{expected:.6f} (tol {tol}). Either the model changed or the "
            f"fixture is stale."
        )


@pytest.mark.parametrize("case_index", range(0, 6), ids=lambda i: f"case_{i}")
def test_top_class_matches_fixture(fixture, model_and_vocab, case_index: int) -> None:
    """End-to-end: argmax over recomputed logits still lands on the same
    class index (and sign id)."""
    model, vocab_signs = model_and_vocab
    case = fixture["cases"][case_index]
    with torch.inference_mode():
        tensor = torch.tensor([case["features_42"]], dtype=torch.float32)
        logits = model(tensor).squeeze(0).numpy()
    top_index = int(np.argmax(logits))
    assert top_index == case["top_class_index"], (
        f"{case['sign_id']}: argmax landed on class {top_index} "
        f"({vocab_signs[top_index]}), pinned was {case['top_class_index']} "
        f"({case['top_class_id']})."
    )
    assert vocab_signs[top_index] == case["top_class_id"]
