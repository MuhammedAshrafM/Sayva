"""Regression + unit tests for `sayva_ml.packs.data_loader`.

Guards the sys.modules-registration behavior — the historical bug was that
`@dataclass` inside a dynamically-loaded pack file crashed with
`AttributeError: 'NoneType' object has no attribute '__dict__'` because
the module wasn't in `sys.modules` when the decorator inspected
`cls.__module__`.
"""

from __future__ import annotations

import sys
import textwrap
from pathlib import Path

import pytest

from sayva_ml.packs.data_loader import load_pack_data_module


def _write_pack(tmp_path: Path, module_name: str, body: str) -> Path:
    """Create a minimal `packs/<code>/data/<module_name>.py` layout under tmp_path."""
    pack_root = tmp_path / "fake_pack"
    (pack_root / "data").mkdir(parents=True)
    (pack_root / "data" / f"{module_name}.py").write_text(body, encoding="utf-8")
    return pack_root


def test_load_pack_data_module_registers_before_exec(tmp_path: Path) -> None:
    """A module whose body defines a `@dataclass` must load without error.

    That's the exact shape that regressed before the sys.modules fix.
    """
    pack_root = _write_pack(
        tmp_path,
        "with_dataclass",
        textwrap.dedent(
            """
            from dataclasses import dataclass

            @dataclass(frozen=True)
            class Thing:
                x: int
                y: int

            SENTINEL = Thing(1, 2)
            """
        ).lstrip(),
    )
    module = load_pack_data_module(pack_root, "with_dataclass")
    assert module.SENTINEL.x == 1
    assert module.SENTINEL.y == 2


def test_load_pack_data_module_leaves_module_in_sys_modules(tmp_path: Path) -> None:
    """A successful load registers the module under its synthetic qualname.

    Callers rely on this so subsequent imports don't re-execute the file
    (which would rebuild expensive per-module state).
    """
    pack_root = _write_pack(
        tmp_path, "simple", "VALUE = 42\n"
    )
    load_pack_data_module(pack_root, "simple")
    assert "_pack_fake_pack_simple" in sys.modules


def test_load_pack_data_module_removes_module_on_exec_failure(tmp_path: Path) -> None:
    """If `exec_module` raises, the half-initialized module must NOT remain in
    sys.modules — otherwise a retry finds the broken shell and skips execution.
    """
    pack_root = _write_pack(
        tmp_path,
        "boom",
        "raise ValueError('intentional test failure')\n",
    )
    with pytest.raises(ValueError, match="intentional test failure"):
        load_pack_data_module(pack_root, "boom")
    assert "_pack_fake_pack_boom" not in sys.modules


def test_load_pack_data_module_missing_file_raises_systemexit(tmp_path: Path) -> None:
    """CLI callers want a clean top-level message, not a stack trace, when a
    caller asks for a pack module that doesn't exist."""
    (tmp_path / "empty_pack" / "data").mkdir(parents=True)
    with pytest.raises(SystemExit, match="no data loader 'missing'"):
        load_pack_data_module(tmp_path / "empty_pack", "missing")
