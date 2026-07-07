"""Multiprocessing-safe MediaPipe hand-landmark extraction workers.

This module lives under `sayva_ml/data/` — a language-neutral, importable
Python package — so `ProcessPoolExecutor` can `spawn` on Windows without
child processes needing to reconstruct a dynamically-imported pack
module. Pack-specific dataset code (e.g. `packs/<code>/data/*.py`) is
loaded via `sayva_ml.packs.data_loader.load_pack_data_module`; that path
lives only in the parent's `sys.modules` and is invisible to spawn'd
children, which is why worker callables cannot live inside a pack
module on Windows.

The extraction pipeline itself is not pack-specific:
  image path → OpenCV decode → MediaPipe Hands → 21 (x, y) landmarks
  → `pre_process_landmark` → 42-float feature vector

Callers that partition images by class (e.g. the ASL Alphabet's
letter-per-directory layout) rely on the parent directory name being
the class id; that convention is enforced by the caller, not this
module.
"""

from __future__ import annotations

from pathlib import Path
from typing import Any

from sayva_ml.preprocessing.landmark import pre_process_landmark

# Path to the MediaPipe HandLandmarker `.task` bundle. We deliberately point
# at the same file the Android app ships with (auto-fetched by
# `./gradlew :shared:downloadHandLandmarkerModel` on first build) so both
# sides run identical model bytes and yield identical landmarks.
_REPO_ROOT = Path(__file__).resolve().parents[4]  # …/ml/src/sayva_ml/data/mediapipe_workers.py → repo root
DEFAULT_HAND_LANDMARKER_TASK = (
    _REPO_ROOT / "shared" / "src" / "androidMain" / "assets" / "mediapipe"
    / "hand_landmarker.task"
)

# Each worker holds one MediaPipe `HandLandmarker` instance for its lifetime.
# Constructing one takes ~200 ms (TFLite model load + graph init) — doing
# that per image would dominate wall-clock. `_MP_HANDS` is process-global
# inside each worker; the main process never touches it.
_MP_HANDS: Any = None


DEFAULT_MIN_HAND_DETECTION_CONFIDENCE = 0.5


def init_worker(
    task_path: str | None = None,
    min_hand_detection_confidence: float = DEFAULT_MIN_HAND_DETECTION_CONFIDENCE,
) -> None:
    """`ProcessPoolExecutor(initializer=…)` hook — build one HandLandmarker
    per worker.

    Uses MediaPipe's Tasks Vision API (`mediapipe.tasks.vision.HandLandmarker`);
    the older `mediapipe.solutions.hands` module was removed after 0.10.14.

    Args:
        task_path: absolute path to a HandLandmarker `.task` bundle. Defaults
            to the Android app's bundled copy so both platforms run identical
            model bytes.
        min_hand_detection_confidence: MediaPipe threshold for accepting a
            candidate hand detection. Lower values catch more hands (at the
            cost of more false detections); higher values are stricter.
            Callers tune this per experiment — the default matches the
            Android runtime for parity.
    """
    global _MP_HANDS
    from mediapipe import Image, ImageFormat  # noqa: F401 — re-exported via detect()
    from mediapipe.tasks import python as mp_python  # type: ignore[import-not-found]
    from mediapipe.tasks.python import vision as mp_vision  # type: ignore[import-not-found]

    resolved = Path(task_path) if task_path else DEFAULT_HAND_LANDMARKER_TASK
    if not resolved.exists():
        raise FileNotFoundError(
            f"MediaPipe HandLandmarker task file missing at {resolved}. "
            f"Run `./gradlew :shared:downloadHandLandmarkerModel` from the "
            f"repo root to fetch it, or pass task_path explicitly."
        )
    options = mp_vision.HandLandmarkerOptions(
        base_options=mp_python.BaseOptions(model_asset_path=str(resolved)),
        num_hands=1,
        min_hand_detection_confidence=min_hand_detection_confidence,
        running_mode=mp_vision.RunningMode.IMAGE,
    )
    _MP_HANDS = mp_vision.HandLandmarker.create_from_options(options)


def extract_one(image_path_str: str) -> tuple[str, list[float] | None]:
    """Worker task: `(image_path)` → `(class_id, preprocessed 42-float or None)`.

    Class id is derived from `image_path.parent.name` — the standard
    ImageFolder-style layout Kaggle datasets ship in. Returns `(class, None)`
    when the image can't be decoded, when no hand is detected, or when the
    detector returns fewer than 21 landmarks (partial hands are dropped
    rather than padded to preserve label integrity).
    """
    import cv2  # type: ignore[import-not-found]
    from mediapipe import Image, ImageFormat  # type: ignore[import-not-found]

    image_path = Path(image_path_str)
    class_id = image_path.parent.name
    image = cv2.imread(image_path_str)
    if image is None:
        return (class_id, None)
    image_rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
    height, width = image_rgb.shape[:2]

    assert _MP_HANDS is not None, (
        "worker not initialized — pass init_worker as ProcessPoolExecutor's "
        "initializer, or call it once directly for single-process use."
    )
    mp_image = Image(image_format=ImageFormat.SRGB, data=image_rgb)
    result = _MP_HANDS.detect(mp_image)
    if not result.hand_landmarks:
        return (class_id, None)
    first = result.hand_landmarks[0]
    raw = [
        [min(int(lm.x * width), width - 1), min(int(lm.y * height), height - 1)]
        for lm in first
    ]
    if len(raw) != 21:
        return (class_id, None)
    return (class_id, pre_process_landmark(raw))
