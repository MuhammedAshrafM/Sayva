"""WLASL clip loader for the temporal sign recognizer (Track C).

Dataset: WLASL — Word-Level American Sign Language.
    https://dxli94.github.io/WLASL/

Requires manual download and license acceptance. The loader expects the
extracted structure at `ml/datasets/wlasl/videos/*.mp4` plus the metadata
JSON provided in the same release.

**Not runnable inside this session** — this file provides the code path so
downstream training can consume the dataset when it lands.

Flow:
    1. Read `nslt_2000.json` (or the split JSON that comes with WLASL) —
       maps video IDs to gloss (English word) + train/val/test split.
    2. Filter to the 5 signs in `configs/sign_toy_v1.yaml`
       (hello, thank_you, please, sorry, yes).
    3. For each clip: OpenCV → per-frame BGR → MediaPipe Hands → up to 2
       hands (`left`, `right`) per frame → `pre_process_two_hand_sequence`
       to `[30, 84]`.
    4. Write a cache `.npz` in `ml/models/cache/wlasl_toy.npz`.

Missing frames (no hand detected): the two-hand preprocessor already returns
zeros for a missing hand — no special handling needed.

Handedness resolution: MediaPipe returns a `handedness.classification[0].label`
of "Left" or "Right". WLASL clips are unmirrored (right-handed presenter's
right hand is on the right side of the frame). We route MediaPipe's `Left` →
our `left`, `Right` → our `right`. Signers who are left-handed will produce
mirrored data — WLASL doesn't tag this and we intentionally don't compensate
in Phase 1.
"""

from __future__ import annotations

import json
import random
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import numpy as np

from sayva_ml.preprocessing.landmark import pre_process_two_hand_sequence
from sayva_ml.vocabulary import Vocabulary


@dataclass(frozen=True)
class WLASLSplit:
    X_train: np.ndarray  # (N, T, 84)
    y_train: np.ndarray
    X_val: np.ndarray
    y_val: np.ndarray
    X_test: np.ndarray
    y_test: np.ndarray
    per_class_counts: dict[str, int]


def _extract_two_hand_frames_from_video(
    video_path: Path,
    max_frames: int,
) -> list[tuple[list[list[float]] | None, list[list[float]] | None]]:
    """Run MediaPipe on each frame of `video_path`, return up to `max_frames` frames.

    Deferred imports — the module stays importable without opencv/mediapipe
    installed on developer machines.
    """
    import cv2  # type: ignore[import-not-found]
    import mediapipe as mp  # type: ignore[import-not-found]

    cap = cv2.VideoCapture(str(video_path))
    if not cap.isOpened():
        raise RuntimeError(f"Cannot open video: {video_path}")

    frames: list[tuple[list[list[float]] | None, list[list[float]] | None]] = []
    with mp.solutions.hands.Hands(
        static_image_mode=False,
        max_num_hands=2,
        min_detection_confidence=0.5,
    ) as hands:
        while True:
            ok, frame = cap.read()
            if not ok:
                break
            if len(frames) >= max_frames:
                break

            image_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            h, w = image_rgb.shape[:2]
            result = hands.process(image_rgb)

            left = None
            right = None
            if result.multi_hand_landmarks and result.multi_handedness:
                for hand_lms, handedness in zip(
                    result.multi_hand_landmarks,
                    result.multi_handedness,
                    strict=True,
                ):
                    label = handedness.classification[0].label
                    coords = [
                        [min(int(lm.x * w), w - 1), min(int(lm.y * h), h - 1)]
                        for lm in hand_lms.landmark
                    ]
                    if label == "Left":
                        left = coords
                    else:
                        right = coords
            frames.append((left, right))
    cap.release()
    return frames


def build_cache(  # noqa: PLR0913
    videos_dir: Path,
    metadata_json: Path,
    vocabulary: Vocabulary,
    cache_path: Path,
    sequence_length: int = 30,
    train_ratio: float = 0.75,
    val_ratio: float = 0.125,
    seed: int = 42,
) -> WLASLSplit:
    """Walk WLASL clips for our 5 signs, extract landmarks, split, cache.

    `metadata_json` is the JSON WLASL ships that maps clip IDs to gloss +
    split assignment. If the exact JSON schema evolves, adjust `_load_entries`
    below — it's the only place that reads it.
    """
    if not videos_dir.exists():
        raise FileNotFoundError(
            f"WLASL videos not found at {videos_dir}. See ml/README.md for download steps."
        )
    if not metadata_json.exists():
        raise FileNotFoundError(f"WLASL metadata not found at {metadata_json}.")

    entries = _load_entries(metadata_json, vocabulary)

    per_class: dict[str, list[list[list[float]]]] = {s.id: [] for s in vocabulary.signs}
    for entry in entries:
        clip_path = videos_dir / f"{entry['video_id']}.mp4"
        if not clip_path.exists():
            continue
        raw_frames = _extract_two_hand_frames_from_video(clip_path, max_frames=120)
        processed = pre_process_two_hand_sequence(raw_frames, target_length=sequence_length)
        per_class[entry["sign_id"]].append(processed)

    all_X: list[list[list[float]]] = []
    all_y: list[int] = []
    counts: dict[str, int] = {}
    for sign_id, samples in per_class.items():
        idx = vocabulary.index_of(sign_id)
        counts[sign_id] = len(samples)
        for s in samples:
            all_X.append(s)
            all_y.append(idx)

    rng = random.Random(seed)
    indices = list(range(len(all_X)))
    rng.shuffle(indices)
    X = np.asarray([all_X[i] for i in indices], dtype=np.float32)
    y = np.asarray([all_y[i] for i in indices], dtype=np.int64)

    n = len(X)
    n_train = int(n * train_ratio)
    n_val = int(n * val_ratio)
    split = WLASLSplit(
        X_train=X[:n_train],
        y_train=y[:n_train],
        X_val=X[n_train : n_train + n_val],
        y_val=y[n_train : n_train + n_val],
        X_test=X[n_train + n_val :],
        y_test=y[n_train + n_val :],
        per_class_counts=counts,
    )
    cache_path.parent.mkdir(parents=True, exist_ok=True)
    np.savez(
        cache_path,
        X_train=split.X_train,
        y_train=split.y_train,
        X_val=split.X_val,
        y_val=split.y_val,
        X_test=split.X_test,
        y_test=split.y_test,
    )
    return split


def _load_entries(metadata_json: Path, vocabulary: Vocabulary) -> list[dict[str, Any]]:
    """Return `[{video_id, sign_id}, ...]` filtered to our vocabulary.

    WLASL's canonical metadata format:
        [{"gloss": "hello", "instances": [{"video_id": "12345", ...}, ...]}, ...]

    We map WLASL's `gloss` field to our vocab `id` via lower/underscore.
    """
    entries: list[dict[str, Any]] = []
    with metadata_json.open("r", encoding="utf-8") as f:
        payload = json.load(f)

    vocab_ids = {s.id for s in vocabulary.signs}
    for gloss_entry in payload:
        gloss = str(gloss_entry.get("gloss", "")).strip().lower().replace(" ", "_")
        if gloss not in vocab_ids:
            continue
        for inst in gloss_entry.get("instances", []):
            video_id = str(inst.get("video_id", "")).strip()
            if video_id:
                entries.append({"video_id": video_id, "sign_id": gloss})
    return entries


def load_cache(cache_path: Path) -> WLASLSplit:
    data = np.load(cache_path)
    return WLASLSplit(
        X_train=data["X_train"],
        y_train=data["y_train"],
        X_val=data["X_val"],
        y_val=data["y_val"],
        X_test=data["X_test"],
        y_test=data["y_test"],
        per_class_counts={},
    )
