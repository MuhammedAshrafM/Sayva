"""Hand landmark preprocessing — the exact math Kazuhito ships in
`hand-gesture-recognition-using-mediapipe/app.py::pre_process_landmark`.

Kotlin mirrors this function in `shared/src/commonMain/kotlin/org/moashraf/sayva/ml/
LandmarkPreprocessor.kt`. Both sides consume the same JSON fixture in
`ml/tests/fixtures/landmark_parity.json` — if they ever drift, the parity test
fails on either side.

The pipeline:
    1. Translate: subtract landmark[0] (the wrist) from every point.
    2. Flatten: [(x0, y0), (x1, y1), ...] → [x0, y0, x1, y1, ...].
    3. Normalize: divide every element by the max absolute component so the
       result lives in [-1, 1] and is scale-invariant.

Input: 21 (x, y) pairs (MediaPipe hand landmark output, ignoring z).
Output: 42-float vector.
"""

from __future__ import annotations

from typing import Sequence


def pre_process_landmark(landmarks: Sequence[Sequence[float]]) -> list[float]:
    """Preprocess 21 hand landmarks into the 42-float model input vector.

    Args:
        landmarks: 21 (x, y) pairs. Any iterable of length-2 iterables works;
            values are treated as floats.

    Returns:
        42 floats. Wrist at (0, 0); max absolute component == 1.0.

    Raises:
        ValueError: if the landmark count is not exactly 21.
    """
    if len(landmarks) != 21:
        raise ValueError(f"Expected 21 landmarks, got {len(landmarks)}")

    base_x, base_y = float(landmarks[0][0]), float(landmarks[0][1])
    relative: list[float] = []
    for p in landmarks:
        relative.append(float(p[0]) - base_x)
        relative.append(float(p[1]) - base_y)

    max_abs = max(abs(v) for v in relative)
    if max_abs == 0.0:
        # Degenerate input: all landmarks at the same point. Kazuhito's code
        # would divide-by-zero here; we return zeros to keep the recognizer
        # from crashing on a bad frame. Callers should treat this as a
        # low-quality frame and skip it — a zero vector will never match any
        # trained class with high confidence.
        return [0.0] * 42

    return [v / max_abs for v in relative]


def pre_process_two_hand_frame(
    left: Sequence[Sequence[float]] | None,
    right: Sequence[Sequence[float]] | None,
) -> list[float]:
    """Preprocess one frame of a two-handed sign into 84 floats.

    Each hand is preprocessed independently (wrist-relative + max-abs
    normalized per hand — same math as `pre_process_landmark`), then
    concatenated `[left_42, right_42]`. A missing hand contributes 42 zeros.

    This is a Phase-1 pipeline-validation design choice: it keeps the code
    trivial and matches how Kazuhito's single-hand recognizer normalizes.
    Phase 2 may switch to a shared-reference scheme that preserves
    inter-hand geometry (see docs/AI_PIPELINE.md).

    Args:
        left, right: 21 (x, y) pairs each, or `None` if that hand isn't
            detected in this frame. Callers are responsible for stable
            handedness assignment across frames (typically via MediaPipe's
            `handedness` field).

    Returns:
        84 floats: `[left_hand_42, right_hand_42]`.
    """
    left_out = pre_process_landmark(left) if left is not None else [0.0] * 42
    right_out = pre_process_landmark(right) if right is not None else [0.0] * 42
    return left_out + right_out


def pre_process_two_hand_sequence(
    frames: Sequence[tuple[Sequence[Sequence[float]] | None, Sequence[Sequence[float]] | None]],
    target_length: int,
) -> list[list[float]]:
    """Preprocess a variable-length two-hand sequence into a fixed `[T, 84]`.

    Handles temporal resampling:
      * If `len(frames) < target_length`, right-pad with zero-vectors.
      * If `len(frames) > target_length`, uniformly sample `target_length`
        indices — spread across the clip, not just the first N.

    Args:
        frames: iterable of `(left, right)` per-frame landmark tuples (either
            of which may be `None`).
        target_length: fixed sequence length the model was trained on
            (currently 30 for `sign_toy_v1`).

    Returns:
        `[T, 84]` list-of-lists. Convert to a numpy array or tensor upstream.
    """
    if target_length <= 0:
        raise ValueError(f"target_length must be positive, got {target_length}")

    if not frames:
        return [[0.0] * 84 for _ in range(target_length)]

    if len(frames) >= target_length:
        step = (len(frames) - 1) / (target_length - 1) if target_length > 1 else 0.0
        indices = [min(round(i * step), len(frames) - 1) for i in range(target_length)]
        out = [pre_process_two_hand_frame(*frames[j]) for j in indices]
    else:
        out = [pre_process_two_hand_frame(*f) for f in frames]
        while len(out) < target_length:
            out.append([0.0] * 84)
    return out
