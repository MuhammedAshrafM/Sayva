#!/usr/bin/env python3
"""Build distributable pack artifacts under Compose resources.

For every pack in `ml/packs/`, this script:
  1. Validates the manifest (schema + files exist + shapes align)
  2. Copies model files into
     `shared/src/commonMain/composeResources/files/language_packs/{code}/models/`
  3. Emits a single `manifest.json` at the same location with vocabularies
     and labels inlined — one file the mobile pack loader parses.

Run:
    cd ml && uv run python scripts/generate_pack.py

Flags:
    --check     Fail if the on-disk `manifest.json` or copied model bytes
                differ from what a fresh regenerate would produce. CI uses
                this to catch stale distributable artifacts.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import sys
from pathlib import Path

_REPO_ROOT = Path(__file__).resolve().parents[2]
_ML_ROOT = _REPO_ROOT / "ml"
sys.path.insert(0, str(_ML_ROOT / "src"))

from sayva_ml.packs.manifest import (  # noqa: E402
    LanguagePackManifest,
    ModelSpec,
    validate_pack,
)
from sayva_ml.packs.registry import discover_packs  # noqa: E402

_COMPOSE_ROOT = (
    _REPO_ROOT
    / "shared"
    / "src"
    / "commonMain"
    / "composeResources"
    / "files"
    / "language_packs"
)


def _model_dist_filename(model: ModelSpec) -> str:
    """Simple stable name we use inside the distributed pack.

    Source paths under `packs/{code}/models/exported/{version}/model.tflite`
    include version dirs; those don't belong in the mobile bundle. Callers
    ship one file per model — the manifest tells the app which is which."""
    return f"{model.id}.tflite"


def _write_json(path: Path, payload: object) -> None:
    """Write JSON with a stable key order and no trailing newlines beyond one.

    Byte-stable output is important for the `--check` mode; unstable ordering
    would trip a false positive on machines with different dict iteration
    behaviour (though modern Python is fine here)."""
    path.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(payload, indent=2, ensure_ascii=False, sort_keys=False) + "\n"
    path.write_text(text, encoding="utf-8")


def _read_json_if_exists(path: Path) -> str | None:
    if not path.exists():
        return None
    return path.read_text(encoding="utf-8")


def _sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


def _model_integrity(manifest: LanguagePackManifest, model: ModelSpec) -> dict:
    """SHA-256 + file size for one model, keyed on the SOURCE bytes.

    We compute against `packs/<code>/<model.file>` rather than the copied
    distributable path so both stay bit-identical when a subsequent
    generator run inspects them. The copy step (below) uses the same
    checksum to decide idempotence.
    """
    src = manifest.pack_root / model.file
    return {
        "sha256": _sha256(src),
        "sizeBytes": src.stat().st_size,
    }


def _build_manifest_json(manifest: LanguagePackManifest) -> dict:
    """Distributable manifest — everything the mobile app needs in one JSON.

    Each model entry carries an `integrity` block with the SHA-256 of the
    exact bytes the app will read at runtime plus its size on disk. Both
    fields are populated NOW so the shape OTA verification will consume is
    already established — retrofitting integrity metadata across shipped
    packs later is expensive; populating today costs nothing (see
    PACKS_WORKFLOW.md, "SHA-256 integrity metadata"). Signing the pack
    remains a Phase 3 concern; the hash is what signing will cover.
    """
    return {
        "schemaVersion": manifest.schema_version,
        "recognitionCode": manifest.recognition_code,
        "displayName": manifest.display_name,
        "version": manifest.version,
        "minAppVersion": manifest.min_app_version,
        "bundled": manifest.bundled,
        "models": [
            {
                "id": m.id,
                "role": m.role,
                "architecture": m.architecture,
                "modelFile": f"models/{_model_dist_filename(m)}",
                "runtime": {"type": m.runtime_type},
                "inferenceStrategy": m.inference_strategy,
                "input": {
                    "shape": list(m.input.shape),
                    "preprocessing": m.input.preprocessing,
                    "maxHands": m.input.max_hands,
                    **(
                        {"sequenceLength": m.input.sequence_length}
                        if m.input.sequence_length is not None
                        else {}
                    ),
                    **(
                        {"twoHandOrdering": m.input.two_hand_ordering}
                        if m.input.two_hand_ordering is not None
                        else {}
                    ),
                },
                "output": {
                    "shape": list(m.output.shape),
                    "postprocessing": m.output.postprocessing,
                },
                "confidenceThresholds": {
                    "show": m.confidence_thresholds.show,
                    "caution": m.confidence_thresholds.caution,
                },
                "vocabulary": {
                    "version": m.vocabulary.version,
                    "signs": [
                        {"id": s.id, "tags": list(s.tags)} for s in m.vocabulary.signs
                    ],
                },
                "integrity": _model_integrity(manifest, m),
            }
            for m in manifest.models
        ],
        "supportedOutputs": list(manifest.supported_outputs),
        "outputLanguageStatus": {
            k: v.value for k, v in manifest.output_language_status.items()
        },
        "defaultOutputLanguage": manifest.default_output_language,
        "outputLabels": manifest.output_labels,
        "ttsLocaleByOutput": manifest.tts_locale_by_output,
        "postProcessing": {
            "spellOutBlankTimeoutMs": manifest.post_processing.spell_out_blank_timeout_ms,
            "sentenceAssemblyRuleset": manifest.post_processing.sentence_assembly_ruleset,
            "capitalization": manifest.post_processing.capitalization,
        },
    }


def _distribute_pack(manifest: LanguagePackManifest, dry_run: bool) -> tuple[bool, list[str]]:
    """Write or verify one pack's distributable artifacts.

    Returns (changed, changed_paths). In dry_run mode, `changed` reflects
    whether a regenerate would rewrite anything; nothing is actually written.
    """
    dist_root = _COMPOSE_ROOT / manifest.recognition_code
    dist_models = dist_root / "models"
    dist_manifest = dist_root / "manifest.json"

    changed_paths: list[str] = []

    # 1. Model files
    for model in manifest.models:
        src = manifest.pack_root / model.file
        dst = dist_models / _model_dist_filename(model)
        if not dst.exists() or _sha256(src) != _sha256(dst):
            changed_paths.append(str(dst.relative_to(_REPO_ROOT)))
            if not dry_run:
                dst.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(src, dst)

    # 2. Manifest JSON
    new_json = json.dumps(
        _build_manifest_json(manifest),
        indent=2, ensure_ascii=False, sort_keys=False,
    ) + "\n"
    existing = _read_json_if_exists(dist_manifest)
    if existing != new_json:
        changed_paths.append(str(dist_manifest.relative_to(_REPO_ROOT)))
        if not dry_run:
            dist_manifest.parent.mkdir(parents=True, exist_ok=True)
            dist_manifest.write_text(new_json, encoding="utf-8")

    return (bool(changed_paths), changed_paths)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--check",
        action="store_true",
        help="Exit 1 if the distributable artifacts on disk are stale.",
    )
    args = parser.parse_args()

    packs = discover_packs().packs
    if not packs:
        print("No packs found in ml/packs/", file=sys.stderr)
        return 1

    any_changed = False
    for manifest in packs:
        validated = validate_pack(manifest.pack_root)
        changed, paths = _distribute_pack(validated, dry_run=args.check)
        if changed:
            any_changed = True
            for p in paths:
                verb = "would rewrite" if args.check else "wrote"
                print(f"{verb}: {p}")
        else:
            print(f"unchanged: language_packs/{manifest.recognition_code}/")

    # Top-level index of every bundled pack — Kotlin side uses this to
    # enumerate available packs at startup (Compose Resources has no directory
    # listing API). Order is stable (packs are sorted at discovery).
    index_payload = {
        "schemaVersion": 1,
        "bundled": [
            {
                "recognitionCode": m.recognition_code,
                "displayName": m.display_name,
                "version": m.version,
                "minAppVersion": m.min_app_version,
            }
            for m in packs
        ],
    }
    index_path = _COMPOSE_ROOT / "index.json"
    index_text = json.dumps(index_payload, indent=2, ensure_ascii=False) + "\n"
    existing_index = _read_json_if_exists(index_path)
    if existing_index != index_text:
        any_changed = True
        rel = index_path.relative_to(_REPO_ROOT)
        if args.check:
            print(f"would rewrite: {rel}")
        else:
            index_path.parent.mkdir(parents=True, exist_ok=True)
            index_path.write_text(index_text, encoding="utf-8")
            print(f"wrote: {rel}")

    if args.check and any_changed:
        print(
            "error: distributable pack artifacts are stale — "
            "run `uv run python scripts/generate_pack.py` and commit.",
            file=sys.stderr,
        )
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
