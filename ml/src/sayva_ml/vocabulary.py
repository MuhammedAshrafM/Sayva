"""Load and validate a language pack's per-model vocabulary YAML.

Vocabularies are the model-facing sign lists — canonical gloss IDs in
class-index order. Human-readable labels live in `labels/{outputCode}.json`
inside each pack, not here. See `docs/architecture/language-packs.md`
(or `plans/language-pack-architecture.md`) for the full contract.

Schema (pack-neutral — every pack, every language uses this shape):

    version: 1
    signs:
      - { id: A, tags: [] }
      - { id: B, tags: [] }

Both the training pipeline (Python) and the Kotlin manifest parser read
this same file to guarantee model outputs and labels stay in sync.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

import yaml


@dataclass(frozen=True)
class Sign:
    """A single sign entry — the model's output class at this index."""

    id: str
    tags: tuple[str, ...] = field(default_factory=tuple)


@dataclass(frozen=True)
class Vocabulary:
    """The loaded vocabulary. Index into `signs` == model output class index."""

    version: int
    signs: tuple[Sign, ...]

    @property
    def size(self) -> int:
        return len(self.signs)

    def index_of(self, sign_id: str) -> int:
        for i, s in enumerate(self.signs):
            if s.id == sign_id:
                return i
        raise KeyError(f"Sign id '{sign_id}' not in vocabulary v{self.version}")


def load_vocabulary(path: Path) -> Vocabulary:
    """Load and validate a vocabulary YAML.

    Raises `ValueError` on malformed input, `FileNotFoundError` if missing.
    """
    with path.open("r", encoding="utf-8") as f:
        raw: dict[str, Any] = yaml.safe_load(f)

    if not isinstance(raw, dict):
        raise ValueError(f"Vocabulary file {path} did not parse as a mapping")
    for required in ("version", "signs"):
        if required not in raw:
            raise ValueError(f"Vocabulary file {path} missing required key '{required}'")

    signs_raw = raw["signs"]
    if not isinstance(signs_raw, list) or not signs_raw:
        raise ValueError(f"Vocabulary file {path} 'signs' must be a non-empty list")

    seen_ids: set[str] = set()
    signs: list[Sign] = []
    for entry in signs_raw:
        if not isinstance(entry, dict):
            raise ValueError(f"Vocabulary sign entry is not a mapping: {entry!r}")
        sid = entry.get("id")
        tags = entry.get("tags", [])
        if not isinstance(sid, str) or not sid:
            raise ValueError(f"Vocabulary sign missing string 'id': {entry!r}")
        if not isinstance(tags, list) or not all(isinstance(t, str) for t in tags):
            raise ValueError(f"Vocabulary sign '{sid}' tags must be a list of strings")
        if sid in seen_ids:
            raise ValueError(f"Vocabulary duplicate sign id: '{sid}'")
        seen_ids.add(sid)
        signs.append(Sign(id=sid, tags=tuple(tags)))

    return Vocabulary(version=int(raw["version"]), signs=tuple(signs))
