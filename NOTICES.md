# Third-Party Notices

This project incorporates work from other open-source projects. The list below
covers assets bundled at build time (models, code, or data). Runtime library
attributions from Gradle-resolved dependencies are surfaced separately by their
own license files.

---

## hand-gesture-recognition-using-mediapipe

- **Source:** https://github.com/Kazuhito00/hand-gesture-recognition-using-mediapipe
- **Author:** Kazuhito Takahashi
- **License:** Apache License 2.0
- **What we use:**
  - `keypoint_classifier.tflite` — bundled as `shared/src/commonMain/composeResources/files/models/smoke_test.tflite` for the Track A on-device runtime smoke test. Not shipped in any user-facing feature.
  - Landmark preprocessing algorithm (`pre_process_landmark` in `app.py`) — ported to `ml/src/sayva_ml/preprocessing/landmark.py` (Python, for training) and reimplemented in Kotlin under `shared/src/commonMain/kotlin/org/moashraf/sayva/ml/` (for on-device inference). Attribution is preserved in source-file headers.
  - MLP architecture from `keypoint_classification.ipynb` — used as the reference architecture for Sayva's Track B fingerspelling model. Retrained from scratch on a different dataset; no weights are inherited.

The full Apache 2.0 license text is available at https://www.apache.org/licenses/LICENSE-2.0.

---

## onnx2tf

- **Source:** https://github.com/PINTO0309/onnx2tf
- **Author:** PINTO0309 (Katsuya Hyodo)
- **License:** MIT
- **What we use:** Build-time tool that converts our PyTorch-exported ONNX
  file to TFLite. Runs on the developer machine / CI, not shipped in the app.

## Kazuhito00 / hand-keypoint-classification-model-zoo

- **Source:** https://github.com/Kazuhito00/hand-keypoint-classification-model-zoo
- **Author:** Kazuhito Takahashi
- **License:** Apache License 2.0
- **What we use:** Reference / inspiration only. We do not ship any of the
  forked-repo weights or code from this zoo. Listed here so a future reader
  looking at the plan document can follow the trail.
