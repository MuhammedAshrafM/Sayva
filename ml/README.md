# Sayva ML

Sign language recognition training and export pipeline for the Sayva KMP app.

This is the Python subproject documented in `docs/phase-1-tickets.md` P1-07 and the parent plan's "Repo Structure Decision" section. It produces the TFLite (Android) and CoreML (iOS) model files consumed by `shared/src/commonMain/composeResources/files/models/`.

## Prerequisites

- Python 3.11 or 3.12 (see `.python-version`)
- [uv](https://docs.astral.sh/uv/) — recommended package manager

## Setup

```bash
cd ml
uv sync                    # installs core + dev dependencies
uv sync --extra export     # add export deps (ONNX, CoreML, ai-edge-torch)
```

## Common commands

```bash
uv run pytest              # run unit tests
uv run ruff check .        # lint
uv run ruff format .       # auto-format
uv run mypy src/           # type-check
```

## Project layout

```
ml/
├── pyproject.toml          Package config, deps, lint/type/test settings
├── configs/
│   └── vocabulary.yaml     SINGLE SOURCE OF TRUTH for sign vocabulary
│                           Edit here → Gradle regenerates Vocabulary.kt on the Kotlin side
├── src/sayva_ml/
│   ├── data/               Dataset loaders (WLASL, MS-ASL, custom clips)
│   ├── preprocessing/      MediaPipe landmark extraction, normalization
│   ├── models/             LSTM / Transformer architectures
│   ├── training/           Train loops, config, checkpoint I/O
│   ├── evaluation/         Accuracy metrics, confusion matrices
│   └── export/             TFLite + CoreML converters
├── datasets/               Gitignored — external storage or DVC
├── models/
│   ├── checkpoints/        Gitignored
│   └── exported/           Versioned TFLite/CoreML → consumed by Gradle
└── tests/
```

## Vocabulary lock

`configs/*.yaml` are the authoritative sign lists — one file per model.
Both the training pipeline (`sayva_ml.vocabulary`) and the Kotlin
`SignRecognizer` (`shared/src/commonMain/kotlin/org/moashraf/sayva/ml/generated/Vocabulary.kt`) read from them. To add or remove a sign, edit the YAML — the Gradle `regenerateVocabulary` task keeps the Kotlin file in sync, and `verifyVocabulary` runs on `check` so CI catches drift.

Current vocabularies:

| File | Model | Signs |
|---|---|---|
| `fingerspelling.yaml` | Track B: single-frame ASL alphabet | 24 (A–Y, skip J and Z) |
| `sign_toy_v1.yaml` | Track C: 5-sign temporal toy | Hello, Thank you, Please, Sorry, Yes |

## Training the fingerspelling MLP (Track B)

Two data sources are supported. The synthetic path proves the whole
train→export→consumption pipeline works without any external download; the
`asl-alphabet` path produces a real usable model.

```bash
# Sanity check — pipeline shape, no external data needed
uv run python -m sayva_ml.training.train_fingerspelling --data synthetic

# Real training — requires the Kaggle ASL Alphabet Dataset downloaded first
# (see below). Produces the model that ships in the app.
uv run python -m sayva_ml.training.train_fingerspelling --data asl-alphabet
```

Both write versioned artifacts to `models/exported/fingerspelling_v<ver>/`:
- `model.pt` — PyTorch checkpoint
- `model.onnx` — Portable graph for TFLite/CoreML export
- `vocabulary.json` — Class labels in output-index order
- `metrics.json` — Train/val/test accuracy
- `config.json` — Hyperparams + git SHA + timestamp

### Preparing the ASL Alphabet Dataset

1. Register at https://kaggle.com and accept the terms for [ASL Alphabet Dataset](https://www.kaggle.com/datasets/grassknoted/asl-alphabet)
2. Download and extract to `ml/datasets/asl_alphabet/asl_alphabet_train/`
3. Build the landmark cache (MediaPipe extraction, one-time):
   ```bash
   uv run python -c "
   from pathlib import Path
   from sayva_ml.data.asl_alphabet import build_cache
   from sayva_ml.vocabulary import load_vocabulary, CONFIGS_DIR
   vocab = load_vocabulary(CONFIGS_DIR / 'fingerspelling.yaml')
   build_cache(Path('datasets/asl_alphabet/asl_alphabet_train/asl_alphabet_train'), vocab, Path('models/cache/asl_alphabet.npz'))
   "
   ```
4. Train: `uv run python -m sayva_ml.training.train_fingerspelling --data asl-alphabet`

## Model export flow

```
train_fingerspelling.py --data ... → model.pt + model.onnx
                                    → export/to_tflite.py  → model.tflite   (Linux/macOS)
                                    → export/to_coreml.py  → model.mlpackage (macOS)
```

The Windows path stops at ONNX because `ai-edge-torch`'s TFLite converter has no Windows wheel. Windows developers commit the `.pt`/`.onnx` and let a Linux/macOS runner produce the TFLite. CoreML export is always macOS-only.

Once the artifacts exist, the Gradle `copyMlModels` task copies them into `shared/src/commonMain/composeResources/files/models/` at build time. That task lands in ticket P1-19b.

## Larger datasets (Track C — temporal signs)

- **WLASL** (Word-Level American Sign Language) — https://dxli94.github.io/WLASL/
  Requires manual download and license acceptance. Place videos under `ml/datasets/wlasl/`.
- **MS-ASL** (Microsoft American Sign Language) — https://www.microsoft.com/en-us/research/project/ms-asl/
  Download instructions on the project page. Place under `ml/datasets/ms-asl/`.

Neither dataset is redistributed with this repo.

## Related documentation

- Parent plan: `C:\Users\amirm\.claude\plans\harmonic-coalescing-pond.md`
- Phase 1 tickets: `../docs/phase-1-tickets.md`
- AI pipeline design: `../docs/AI_PIPELINE.md`
