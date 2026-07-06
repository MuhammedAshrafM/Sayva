# Language Pack Workflow

> Developer workflow and automation roadmap for Sayva Language Packs.
> This document describes how **any** Language Pack — regardless of the
> sign language it serves — is authored, validated, packaged, and
> eventually published. Every pack follows the same lifecycle;
> adding a new language means adding a directory under `ml/packs/`,
> not changing this workflow.

---

## Table of Contents

- [Scope and principles](#scope-and-principles)
- [Anatomy of a Language Pack](#anatomy-of-a-language-pack)
- [Phase 1 — Explicit developer command (current)](#phase-1--explicit-developer-command-current)
- [Phase 2 — Verify-before-build](#phase-2--verify-before-build)
- [Phase 3 — Three independent CI pipelines](#phase-3--three-independent-ci-pipelines)
- [Cross-cutting policies](#cross-cutting-policies)
- [How to add a new Language Pack](#how-to-add-a-new-language-pack)

---

## Scope and principles

This workflow governs the boundary between:

- **Pack authoring** — updating `ml/packs/<pack_code>/manifest.yaml`,
  vocabularies, labels, or exported model files.
- **Pack distribution** — the app-consumable JSON + model bundle under
  `shared/src/commonMain/composeResources/files/language_packs/<pack_code>/`.

Guiding principles the phases below all preserve:

1. **Language-agnostic.** No task, workflow file, or check hardcodes a
   specific sign language. Every command walks `ml/packs/*` uniformly.
2. **Model-agnostic.** MLP, LSTM, Transformer, CTC — each pack declares
   its own runtime, preprocessing, and postprocessing adapter IDs. The
   pipeline reads adapter IDs from the manifest; the workflow only
   validates the shape.
3. **Deterministic builds.** The build never mutates project artifacts.
   Regeneration is always explicit. Builds only *verify* freshness.
4. **Fast feedback per cadence.** Pack changes iterate faster than model
   training. Their CI must not wait on training or heavy ML tooling.
5. **Version is contract.** A pack's `version` is authoritative for
   analytics, runtime comparisons, and (once OTA lands) client-side
   cache-busting. Every touched pack bumps its version. Every bumped
   version is valid SemVer 2.0.0.

---

## Anatomy of a Language Pack

Every Language Pack — for any sign language — has the same layout:

```
ml/packs/<pack_code>/
├── manifest.yaml                # Source of truth (hand-authored)
├── vocabularies/
│   └── <model_id>.yaml          # One vocabulary file per model
├── labels/
│   └── <output_lang>.json       # One JSON per supported output language
├── models/
│   └── exported/<model_id>_<version>/model.tflite
├── data/                        # Optional — dataset loaders / adapters
└── training_configs/            # Optional — model training configs
```

The distributed form the app actually loads:

```
shared/src/commonMain/composeResources/files/language_packs/
├── index.json                                  # Top-level catalog
└── <pack_code>/
    ├── manifest.json                           # Generated from manifest.yaml
    └── models/
        └── <model_id>.tflite                   # Copied from ml/packs/…/exported/
```

Everything in the second tree is a build artifact. Nothing in it is
hand-edited; the regeneration step is what keeps it in sync with the
authoring tree.

---

## Phase 1 — Explicit developer command (current)

**Goal.** One command regenerates the distributed artifacts for every
pack. Every developer runs the same command after modifying any pack.

**Available today.**

```bash
cd ml && uv run python scripts/generate_pack.py
```

This walks `ml/packs/*`, validates each manifest, copies model files,
and writes `manifest.json` + `index.json` under Compose resources.

**Target for Phase 1 completion.** Wrap the Python invocation in a
first-class Gradle task so the entire dev surface is uniform:

| Task | Behavior |
|---|---|
| `./gradlew generatePacks` | Regenerates every pack's distributed artifacts. Writes files. |
| `./gradlew verifyPacks` | Dry-run check — fails if regeneration would rewrite anything. Writes nothing. |

Under the hood both tasks shell into `scripts/generate_pack.py`
(`--check` for the verify variant). The Gradle wrapper exists so the
command surface matches every other build task in the project.

**Rule.** Neither task is wired into `assemble` yet. Developers run
`generatePacks` themselves; CI runs `verifyPacks` (in Phase 3) as a
guard.

---

## Phase 2 — Verify-before-build

**Goal.** Make it impossible to build the app with stale pack artifacts
without also modifying the build process to mutate project files.

**Wiring.**

```
:shared:assemble  →  :shared:verifyPacks  →  fail if stale
```

Same pattern as the existing `verifyVocabulary` guard: the build's job
is to detect a mismatch and stop, not to fix it. If `verifyPacks`
fails, the developer runs `./gradlew generatePacks`, commits the
regenerated artifacts, and re-tries.

**What we deliberately do NOT do:**

- **No autogeneration during build.** Builds must be deterministic.
  A build that mutates tracked files gives non-idempotent behavior,
  dirties working trees in CI, and hides "I forgot to commit"
  mistakes behind a green local run.
- **No implicit fallbacks.** A missing pack is an error, not a
  degradation-to-defaults. The pipeline surfaces a clear
  build-time failure so the fix is obvious.

**Trigger.** `verifyPacks` runs when any of the following change,
detected via Gradle input tracking:

- `ml/packs/**` (manifest, vocabulary, labels, models)
- `ml/scripts/generate_pack.py`
- `ml/src/sayva_ml/packs/**` (parser / registry code)

Unrelated ML changes (training configs, notebooks, dataset scripts)
do not trigger it — they can't affect the distributed artifacts.

---

## Phase 3 — Three independent CI pipelines

**Goal.** Split automation by *cadence* — how often each concern
changes — so pack authors, app engineers, and ML engineers each get
sub-target feedback loops.

### The three pipelines

| Pipeline | Trigger paths | Runtime | Timeout target |
|---|---|---|---|
| `app-ci` | `androidApp/**`, `iosApp/**`, `shared/**`, Gradle files, `.github/workflows/app-ci.yml` | Ubuntu + macOS | ≤ 30 min |
| `packs-ci` | `ml/packs/**`, `ml/scripts/generate_pack.py`, `ml/src/sayva_ml/packs/**`, `ml/src/sayva_ml/vocabulary/**`, `.github/workflows/packs-ci.yml` | Ubuntu | ≤ 5 min |
| `ml-ci` | `ml/src/sayva_ml/**` (excluding `packs/`), `ml/tests/**`, `ml/scripts/train_*.py`, `.github/workflows/ml-ci.yml` | Ubuntu (heavy) | ≤ 30 min |

The `app-ci` pipeline exists today. The `ml-ci` pipeline exists today
but currently owns *both* training and pack duties; Phase 3 splits it.

### `packs-ci` — Language Pack pipeline

Runs on every PR that touches pack-authoring surface. Every step must
be language-agnostic and must walk every pack under `ml/packs/*`
uniformly.

**Steps:**

1. **Manifest schema validation.** Every `manifest.yaml` loads via
   `sayva_ml.packs.manifest.load_manifest`. Schema violations
   (missing fields, invalid SemVer, unknown enums, canonical output
   coverage) fail the pipeline.
2. **Label / vocabulary consistency.** Every supported output ships a
   labels file. Every labels file covers every model's vocabulary. No
   orphan sign IDs, no missing translations declared `complete`.
3. **Model file verification.** Every referenced `.tflite` (or other
   runtime format a future pack picks) exists at the declared path
   inside `ml/packs/<pack_code>/models/exported/…`.
4. **Distributable freshness.** `generate_pack.py --check` — the
   distributed `manifest.json` + `index.json` + copied model bytes
   must match what a fresh regenerate would produce.
5. **SHA-256 integrity metadata.** Confirm every model file in the
   distributed bundle has a matching `contentChecksum` field in its
   distributed `manifest.json` (see [Cross-cutting policies](#cross-cutting-policies)).
6. **Version-bump enforcement.** `verify_pack_version_bump.py` — any
   pack whose directory has any diff between base and head must have
   bumped its `manifest.yaml` version.
7. **(Future) Pack signing.** Once OTA distribution ships, this
   pipeline signs the distributable bundle and uploads it to the
   OTA catalog.

### `ml-ci` — Training and model-quality pipeline

Runs on changes to training code, dataset loaders, and shared ML
utilities that are *not* per-pack. This is the current `ml-ci` minus
the pack-authoring concerns moved into `packs-ci`.

**Steps:** ruff, mypy, pytest with coverage, vocabulary codegen
freshness, landmark parity fixture freshness, synthetic-training
smoke tests for each supported architecture family.

### `app-ci` — Application pipeline

Unchanged from today: JDK + Gradle setup, `verifyVocabulary`,
`assemble`, `testAndroidHostTest`, `assembleDebug`, iOS framework
build + tests.

### Why three, not two

Merging `packs-ci` into `ml-ci` makes label-only edits wait behind a
20-minute training runner. Merging `packs-ci` into `app-ci` makes pure
Kotlin PRs run pack validation they don't need. Three pipelines let
each concern own its trigger paths and its runtime budget.

---

## Cross-cutting policies

These apply across every phase and every Language Pack.

### Version is the cache-busting contract

- Every `manifest.yaml` `version` is valid SemVer 2.0.0 — enforced by
  `sayva_ml.packs.manifest.load_manifest` (build-time) and by
  `PackManifestParser` (runtime, defense in depth for downloaded packs).
- Every PR that touches any file under `ml/packs/<pack_code>/**` bumps
  that pack's version — enforced by
  `ml/scripts/verify_pack_version_bump.py` in `packs-ci`.
- Once OTA distribution ships, the client uses `version` to decide
  whether an installed pack is stale. The check that keeps that
  contract honest is the same check we run today.

### SHA-256 integrity metadata

Populated during `generatePacks` starting now, not deferred to OTA:

```json
"contentChecksum": "sha256:<hex>",
"sizeBytes": {
  "compressed": <int>,
  "uncompressed": <int>
}
```

**Why now.** The hook exists (see `generate_pack.py`), it costs
nothing to populate, and it establishes the shape OTA integrity
verification will consume. Waiting until OTA arrives means one of two
things: retrofitting metadata across every shipped pack, or accepting
a first release without integrity checks. Populating today avoids
both.

**Signing** — the step that turns integrity metadata into
tamper-evidence — remains a Phase 3 concern. Key management is what
makes signing painful, not the hash.

### Build never mutates project artifacts

`assemble` verifies; it does not regenerate. `generatePacks` is
always an explicit developer action. This applies to every
future step added to the workflow — if a step wants to modify a
tracked file, it either belongs in `generatePacks` (explicit,
opt-in) or it's the wrong shape.

### CI file paths never enumerate languages

Every workflow trigger, every script, every Gradle task walks
`ml/packs/*` and processes each entry uniformly. No pipeline knows
which sign languages exist; each pipeline discovers them from the
filesystem at run time. Adding a new pack requires zero CI changes.

---

## How to add a new Language Pack

Adding a pack for any sign language is entirely a **content** action —
no workflow changes, no CI changes, no application code changes.

1. **Create the pack directory.**
   ```
   ml/packs/<pack_code>/
   ├── manifest.yaml
   ├── vocabularies/<model_id>.yaml
   ├── labels/<output_lang>.json     # one per canonical output; stub is fine
   └── models/exported/<model_id>_<version>/model.tflite
   ```
   `<pack_code>` is the ISO 639-3 sign-language code.

2. **Author `manifest.yaml`.** Declare `schemaVersion`, `displayName`
   for every supported output, `version` (valid SemVer), the models
   the pack ships, each model's runtime + preprocessing + postprocessing
   adapter IDs, `confidenceThresholds`, and (for two-hand models) a
   `twoHandOrdering`.

3. **Populate labels for every canonical output.** Every canonical
   output listed in `sayva_ml.packs.manifest.CANONICAL_OUTPUTS` needs
   a `labels/<output_lang>.json` file. Values may be `null` (stub);
   `outputLanguageStatus` records completeness so the runtime knows
   whether to fall back.

4. **Run `./gradlew generatePacks`.** Distributed `manifest.json`,
   `index.json`, and model files land under Compose resources.

5. **Verify.** `./gradlew verifyPacks` should exit clean.
   `./gradlew :shared:testAndroidHostTest` should exit clean —
   the language-pack contract tests walk every pack.

6. **Commit.** `ml/packs/<pack_code>/**` and the regenerated
   `shared/src/commonMain/composeResources/files/language_packs/…` go
   in the same commit.

If any adapter ID declared in the manifest isn't in the app's
registry yet, the model will fail to load with a clear
"requires app v N.M+" error at pack-load time. Ship the adapter in
the app first, or pin the pack's `minAppVersion` accordingly.
