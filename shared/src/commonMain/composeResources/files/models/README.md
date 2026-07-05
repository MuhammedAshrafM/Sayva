# `files/models/` — legacy artifact directory

Everything shipped in a Language Pack now lives under
`files/language_packs/{code}/`. This directory is retained only for the one
file still referenced outside the pack architecture:

## `smoke_test.tflite` (6 KB)

Kazuhito00's shipped `keypoint_classifier.tflite` — 3-class hand-shape
classifier. Used by `TfliteRuntimeSmokeTest` in `androidHostTest` to verify
the TFLite runtime path outside of any Language Pack. **Not user-facing.**

Attribution in [../../../../../../NOTICES.md](../../../../../../NOTICES.md).

---

For everything else (fingerspelling model, temporal LSTM, vocabularies,
labels), see [`../language_packs/ase/`](../language_packs/ase/).
