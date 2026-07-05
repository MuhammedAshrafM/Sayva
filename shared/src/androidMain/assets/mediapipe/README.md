# MediaPipe assets

The `HandDetector` on Android requires the MediaPipe HandLandmarker model
file to live at:

    shared/src/androidMain/assets/mediapipe/hand_landmarker.task

This directory is loaded by CameraX at runtime; the file **is not
committed to git** (both to avoid a ~7 MB binary in the repo and because
Google may rotate their published model with newer training runs). Every
developer downloads it once.

## One-time download

```bash
curl -L -o shared/src/androidMain/assets/mediapipe/hand_landmarker.task \
  https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task
```

The file is about **7 MB**. Expected SHA-256 (as of 2026-07):
`61b5f7d92dd91b76a2fbd076cc4a37c6c98f75e3a9f89b39ea7b30f6b4c8b5c1`
(verify against Google's Model Card if you want belt-and-suspenders).

## What happens if it's missing

`HandLandmarker.createFromOptions(...)` throws `MediaPipeException` with
"Unable to open file: mediapipe/hand_landmarker.task". The message points
at this README.

## CI

CI (`.github/workflows/app-ci.yml`) fetches the model in a step before
`:androidApp:assembleDebug` so PR builds don't require a committed binary.
See the `Download MediaPipe HandLandmarker` step in the app-ci workflow.
