#!/usr/bin/env python3
"""Generate the shared golden-inference fixture used by both Python and
Kotlin regression tests.

Output paths (both maintained in lockstep):

    ml/tests/fixtures/golden_inference.json
    shared/src/androidHostTest/resources/golden_inference.json

Purpose
-------
Any train/serve mismatch in the fingerspelling recognizer would show up as
a divergence between the two sides on a small pinned set of hand-selected
inputs. The fixture carries:

  * `raw_landmarks_21` — the 21 (x, y) pairs MediaPipe would produce for
    a real image of each sign. Extracted from the first ASL Alphabet
    training image where MediaPipe successfully detects a hand.
  * `features_42` — the wrist-relative max-abs-normalized vector the
    model was actually trained on. Bit-for-bit output of
    `pre_process_landmark(raw_landmarks_21)`.
  * `logits_24` — the model's raw output tensor for `features_42`.
    Both sides must match here bit-for-bit modulo float epsilon.
  * `top_class_index` / `top_class_id` — argmax over `logits_24` and the
    corresponding vocabulary sign id. Golden expected label.

Layered checks the fixture unlocks:

  * Preprocessing parity: reprocess `raw_landmarks_21` on each side and
    compare to `features_42`.
  * Model parity: feed `features_42` through each side's ML runtime and
    compare to `logits_24`.
  * End-to-end label parity: argmax + vocabulary lookup must land on
    `top_class_id`.

If either the preprocessing or model check diverges, that's the specific
mismatch site — not "something in the pipeline is off".

Usage
-----
    cd ml && uv run --extra export python scripts/generate_golden_inference.py \\
        --dataset-dir "../datasets/asl-alphabet/asl_alphabet_train"

Regenerate every time the model retrains OR any preprocessing / vocabulary
step changes. The fixture is committed; CI compares against the committed
copy the same way it does for `landmark_parity.json`.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import sys
from dataclasses import dataclass
from pathlib import Path

import numpy as np
import torch

_REPO_ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(_REPO_ROOT / "ml" / "src"))

from sayva_ml.data.mediapipe_workers import (  # noqa: E402
    DEFAULT_HAND_LANDMARKER_TASK,
    extract_one,
    init_worker,
)
from sayva_ml.models.fingerspelling_mlp import FingerspellingMLP  # noqa: E402
from sayva_ml.packs.registry import DEFAULT_PACKS_ROOT  # noqa: E402
from sayva_ml.preprocessing.landmark import pre_process_landmark  # noqa: E402
from sayva_ml.vocabulary import load_vocabulary  # noqa: E402

_ML_ROOT = _REPO_ROOT / "ml"
_FIXTURE_PATH = _ML_ROOT / "tests" / "fixtures" / "golden_inference.json"
_KOTLIN_FIXTURE_PATH = (
    _REPO_ROOT
    / "shared"
    / "src"
    / "androidHostTest"
    / "resources"
    / "golden_inference.json"
)

# Signs the fixture pins. Choice rationale:
#   * A, B, C — top of the alphabet + C is where the on-device bias
#     collapses toward. If the bias is a train/serve mismatch we want it
#     visible here.
#   * L — the one letter the user reports as still recognizable on device.
#     Its distinctive shape makes it the natural "positive control".
#   * M, N — the fingers-folded pair MediaPipe struggles with; also the
#     letters M/N with the highest confusion under class imbalance. Both
#     stress the model at its known weak points.
_TARGET_SIGNS: tuple[str, ...] = ("A", "B", "C", "L", "M", "N")


@dataclass(frozen=True)
class _Case:
    sign_id: str
    source_image: str
    image_width: int
    image_height: int
    raw_landmarks_21: list[list[int]]
    features_42: list[float]
    logits_24: list[float]
    top_class_index: int
    top_class_id: str
    top_softmax: float


def _first_hand_detected_image(dataset_dir: Path, sign_id: str) -> tuple[Path, tuple[str, list[float]]]:
    """Walk `dataset_dir/<sign_id>/*.jpg` in filename order and return the
    first (path, extract_one output) where MediaPipe detects a hand.

    We start from the first image so the fixture is reproducible: regenerating
    on any machine picks the same file. If MediaPipe's threshold behavior
    shifts (a new model version) and the previously-picked image no longer
    detects, the fixture will change — that's the point of the regression.
    """
    letter_dir = dataset_dir / sign_id
    if not letter_dir.is_dir():
        raise SystemExit(f"Dataset missing letter directory: {letter_dir}")
    for image_path in sorted(letter_dir.glob("*.jpg")):
        letter, features = extract_one(str(image_path))
        if features is not None:
            return image_path, (letter, features)
    raise SystemExit(
        f"No image under {letter_dir} produced a MediaPipe hand detection — "
        f"can't pin a golden case for {sign_id!r}."
    )


def _raw_landmarks_from_image(image_path: Path) -> tuple[list[list[int]], int, int]:
    """Re-run MediaPipe on `image_path` and return the raw 21 (x, y) pairs
    BEFORE pre_process_landmark, plus the image dimensions.

    Uses the same code path `extract_one` walks (init_worker → HandLandmarker
    → detect), so the produced landmarks are what training saw for this
    exact image.
    """
    import cv2

    from mediapipe import Image, ImageFormat
    from mediapipe.tasks import python as mp_python  # type: ignore[import-not-found]
    from mediapipe.tasks.python import vision as mp_vision  # type: ignore[import-not-found]

    options = mp_vision.HandLandmarkerOptions(
        base_options=mp_python.BaseOptions(
            model_asset_path=str(DEFAULT_HAND_LANDMARKER_TASK)
        ),
        num_hands=1,
        # Matches the current mediapipe_workers.py default; keep in lockstep
        # when adjusting that constant.
        min_hand_detection_confidence=0.3,
        running_mode=mp_vision.RunningMode.IMAGE,
    )
    detector = mp_vision.HandLandmarker.create_from_options(options)

    image = cv2.imread(str(image_path))
    image_rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
    height, width = image_rgb.shape[:2]
    mp_image = Image(image_format=ImageFormat.SRGB, data=image_rgb)
    result = detector.detect(mp_image)
    detector.close()

    if not result.hand_landmarks:
        raise RuntimeError(f"Re-detection failed on {image_path}")

    first = result.hand_landmarks[0]
    raw = [
        [
            min(int(lm.x * width), width - 1),
            min(int(lm.y * height), height - 1),
        ]
        for lm in first
    ]
    if len(raw) != 21:
        raise RuntimeError(f"Expected 21 landmarks from {image_path}, got {len(raw)}")
    return raw, width, height


def _display_path(p: Path) -> str:
    """Repo-relative POSIX path when possible; else absolute POSIX. Never
    crashes on inputs outside the repo (dataset dir usually is)."""
    try:
        return str(p.resolve().relative_to(_REPO_ROOT)).replace("\\", "/")
    except ValueError:
        return str(p.resolve()).replace("\\", "/")


def _build_case(
    dataset_dir: Path,
    sign_id: str,
    model: FingerspellingMLP,
    vocab_signs: list[str],
) -> _Case:
    image_path, _ = _first_hand_detected_image(dataset_dir, sign_id)
    raw_landmarks, width, height = _raw_landmarks_from_image(image_path)
    features = pre_process_landmark(raw_landmarks)

    with torch.inference_mode():
        model.eval()
        tensor = torch.tensor([features], dtype=torch.float32)
        logits = model(tensor).squeeze(0).tolist()

    top_index = int(np.argmax(logits))
    top_id = vocab_signs[top_index]
    top_softmax = float(np.exp(logits[top_index]) / np.sum(np.exp(logits)))

    return _Case(
        sign_id=sign_id,
        source_image=_display_path(image_path),
        image_width=width,
        image_height=height,
        raw_landmarks_21=raw_landmarks,
        features_42=features,
        logits_24=logits,
        top_class_index=top_index,
        top_class_id=top_id,
        top_softmax=top_softmax,
    )


def _write_fixture(payload: dict[str, object]) -> None:
    text = json.dumps(payload, indent=2) + "\n"
    _FIXTURE_PATH.parent.mkdir(parents=True, exist_ok=True)
    _FIXTURE_PATH.write_text(text, encoding="utf-8")
    _KOTLIN_FIXTURE_PATH.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(_FIXTURE_PATH, _KOTLIN_FIXTURE_PATH)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--dataset-dir",
        type=Path,
        required=True,
        help="Path to the ASL Alphabet training tree (dir with A/, B/, C/, … subdirs).",
    )
    parser.add_argument(
        "--pack",
        default="ase",
        help="Recognition-language pack code.",
    )
    parser.add_argument(
        "--model-version",
        default="fingerspelling_v0.2.0",
        help="Model directory under packs/{pack}/models/exported/.",
    )
    args = parser.parse_args()

    pack_root = DEFAULT_PACKS_ROOT / args.pack
    vocab = load_vocabulary(pack_root / "vocabularies" / "fingerspelling.yaml")
    vocab_signs = [s.id for s in vocab.signs]
    vocab_hash = hashlib.sha256(
        ",".join(vocab_signs).encode("utf-8")
    ).hexdigest()

    model_dir = pack_root / "models" / "exported" / args.model_version
    checkpoint = model_dir / "model.pt"
    if not checkpoint.exists():
        raise SystemExit(
            f"Missing {checkpoint}. Run train_fingerspelling.py --pack {args.pack} "
            f"--version {args.model_version} first."
        )

    model = FingerspellingMLP(num_classes=len(vocab_signs))
    model.load_state_dict(torch.load(checkpoint, map_location="cpu"))
    model.eval()

    init_worker()
    cases: list[_Case] = []
    for sign_id in _TARGET_SIGNS:
        if sign_id not in vocab_signs:
            raise SystemExit(
                f"Sign {sign_id!r} not in vocabulary — update _TARGET_SIGNS or "
                f"regenerate against the current pack."
            )
        print(f"Building case for {sign_id}…")
        cases.append(_build_case(args.dataset_dir, sign_id, model, vocab_signs))

    payload = {
        "schema_version": 1,
        "description": (
            "Golden inference regression fixture. Python and Kotlin must both "
            "reproduce `logits_24` (and therefore `top_class_index` / "
            "`top_class_id`) from `features_42`, within 1e-4 per element. "
            "The fixture also carries `raw_landmarks_21` so preprocessing "
            "parity can be re-checked directly against the same inputs the "
            "model was fed."
        ),
        "pack_code": args.pack,
        "model_version": args.model_version,
        "vocab_hash": f"sha256:{vocab_hash}",
        "vocab_order": vocab_signs,
        "tolerance_features": 1e-6,
        "tolerance_logits": 1e-4,
        "cases": [
            {
                "sign_id": c.sign_id,
                "source_image": c.source_image,
                "image_width": c.image_width,
                "image_height": c.image_height,
                "raw_landmarks_21": c.raw_landmarks_21,
                "features_42": c.features_42,
                "logits_24": c.logits_24,
                "top_class_index": c.top_class_index,
                "top_class_id": c.top_class_id,
                "top_softmax": c.top_softmax,
            }
            for c in cases
        ],
    }
    _write_fixture(payload)

    print()
    print(f"wrote: {_FIXTURE_PATH.relative_to(_REPO_ROOT)}")
    print(f"       {_KOTLIN_FIXTURE_PATH.relative_to(_REPO_ROOT)}")
    print()
    print("Per-case argmax vs true label:")
    for c in cases:
        marker = "OK" if c.top_class_id == c.sign_id else "MISS"
        print(
            f"  {c.sign_id}: model -> {c.top_class_id!r:>4} "
            f"(softmax {c.top_softmax * 100:.1f}%)  [{marker}]"
        )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
