"""Filesystem discovery of packs under `ml/packs/`.

The registry is deliberately dumb — it walks a directory, loads every valid
manifest, and returns the results. No filtering, no caching, no side effects.
CI and codegen scripts both use it.
"""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

from sayva_ml.packs.manifest import LanguagePackManifest, load_manifest

_REPO_ROOT = Path(__file__).resolve().parents[3].parent  # ml/src/sayva_ml/packs → repo root
DEFAULT_PACKS_ROOT = _REPO_ROOT / "ml" / "packs"


@dataclass(frozen=True)
class PackRegistry:
    """Snapshot of all packs on disk at discovery time. Immutable."""

    root: Path
    packs: tuple[LanguagePackManifest, ...]

    def by_code(self, recognition_code: str) -> LanguagePackManifest | None:
        for p in self.packs:
            if p.recognition_code == recognition_code:
                return p
        return None


def discover_packs(root: Path | None = None) -> PackRegistry:
    """Walk `root` (default `ml/packs/`) and load every valid pack.

    Any directory containing a `manifest.yaml` is treated as a pack. Malformed
    packs raise — silently skipping a broken pack would mask errors that CI
    should catch.
    """
    packs_root = root or DEFAULT_PACKS_ROOT
    if not packs_root.exists():
        raise FileNotFoundError(f"Packs root not found: {packs_root}")
    packs: list[LanguagePackManifest] = []
    for entry in sorted(packs_root.iterdir()):
        if not entry.is_dir():
            continue
        if not (entry / "manifest.yaml").exists():
            continue
        packs.append(load_manifest(entry))
    return PackRegistry(root=packs_root, packs=tuple(packs))
