#!/usr/bin/env python3
"""Fail if any Language Pack changed without a manifest version bump.

Run against a PR by passing the base and head refs — CI wires this
up in `.github/workflows/ml-ci.yml`. Locally, you can invoke:

    uv run python scripts/verify_pack_version_bump.py \\
        --base origin/main --head HEAD

Contract enforced (per production audit P1 #8):
  * For every pack under `ml/packs/PACK/` that has any file diff between
    `--base` and `--head`, the pack's `manifest.yaml` version MUST have
    changed.
  * A newly added pack passes automatically — there's no prior version to
    compare against.
  * A removed pack is ignored — it can't ship a bump.
  * Version format itself is validated by `sayva_ml.packs.manifest.
    load_manifest` at build time; this script only compares two strings.

Exit codes:
  0 — every touched pack bumped its version.
  1 — one or more packs changed without bumping. Message names the pack
      and the current version so the fix is obvious.
  2 — usage / infrastructure error (bad ref, git failure, malformed
      manifest). Distinct from 1 so CI can tell "your PR failed" from
      "the check itself broke."
"""

from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path

import yaml

_REPO_ROOT = Path(__file__).resolve().parents[2]
_PACKS_ROOT = _REPO_ROOT / "ml" / "packs"


def _git(*args: str) -> str:
    """Run git and return stdout. Raises on non-zero exit."""
    result = subprocess.run(
        ["git", *args],
        capture_output=True,
        text=True,
        cwd=_REPO_ROOT,
        check=False,
    )
    if result.returncode != 0:
        raise RuntimeError(
            f"git {' '.join(args)} failed with exit {result.returncode}: "
            f"{result.stderr.strip()}"
        )
    return result.stdout


def _changed_files(base: str, head: str) -> list[str]:
    """Files that differ between base and head, repo-relative POSIX paths.

    Uses the three-dot syntax so we compare head against the merge base,
    which matches how a PR diff on GitHub works — file-touching commits on
    main that aren't in the PR don't count as "changes in this PR."
    """
    raw = _git("diff", "--name-only", f"{base}...{head}")
    return [line.strip() for line in raw.splitlines() if line.strip()]


def _packs_changed(files: list[str]) -> set[str]:
    """Pack codes whose directory contains any changed file."""
    codes: set[str] = set()
    for path in files:
        parts = path.split("/")
        if len(parts) < 3:
            continue
        if parts[0] != "ml" or parts[1] != "packs":
            continue
        codes.add(parts[2])
    return codes


def _read_manifest_at(ref: str, pack_code: str) -> dict | None:
    """Load `ml/packs/PACK/manifest.yaml` at the given git ref.

    Returns None if the manifest doesn't exist at that ref (new pack, or
    the pack has been removed). Raises on malformed YAML — that's an
    infra failure worth flagging separately.
    """
    path = f"ml/packs/{pack_code}/manifest.yaml"
    try:
        raw = _git("show", f"{ref}:{path}")
    except RuntimeError:
        return None
    parsed = yaml.safe_load(raw)
    if not isinstance(parsed, dict):
        raise ValueError(f"{ref}:{path}: manifest did not parse as a mapping")
    return parsed


def _version_of(manifest: dict | None) -> str | None:
    if manifest is None:
        return None
    version = manifest.get("version")
    return None if version is None else str(version).strip()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", required=True, help="Base git ref (e.g. origin/main).")
    parser.add_argument("--head", required=True, help="Head git ref (e.g. HEAD).")
    args = parser.parse_args()

    try:
        files = _changed_files(args.base, args.head)
    except RuntimeError as e:
        print(f"::error::{e}", file=sys.stderr)
        return 2

    touched = _packs_changed(files)
    if not touched:
        print("No language pack files changed — nothing to check.")
        return 0

    print(f"Packs touched by this diff: {sorted(touched)}")

    violations: list[str] = []
    for code in sorted(touched):
        try:
            head_manifest = _read_manifest_at(args.head, code)
            base_manifest = _read_manifest_at(args.base, code)
        except ValueError as e:
            print(f"::error::{e}", file=sys.stderr)
            return 2

        if head_manifest is None:
            # Pack deleted in HEAD — no version to bump, and the pack is
            # gone. That's a valid PR shape; skip.
            print(f"  {code}: removed at HEAD, skipping.")
            continue

        head_version = _version_of(head_manifest)
        base_version = _version_of(base_manifest)

        if base_version is None:
            # New pack — no prior version to bump. Its own SemVer validity
            # is enforced by load_manifest, not by this diff.
            print(f"  {code}: new pack at version {head_version!r}, skipping.")
            continue

        if head_version == base_version:
            violations.append(
                f"  {code}: files changed but manifest.yaml version stayed at "
                f"{head_version!r}. Bump `version:` in ml/packs/{code}/manifest.yaml."
            )
        else:
            print(f"  {code}: {base_version!r} → {head_version!r} ✓")

    if violations:
        print("::error::Language pack changes without a version bump:", file=sys.stderr)
        for v in violations:
            print(v, file=sys.stderr)
        return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
