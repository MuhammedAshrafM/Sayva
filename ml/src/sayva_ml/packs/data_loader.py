"""Dynamic loader for per-pack Python data modules.

Language Packs live under `ml/packs/<pack_code>/` as data + optional Python
code — deliberately NOT under the `sayva_ml` package tree, so the
training/build scripts stay language-neutral. When a script needs to reach
into a pack (e.g. to invoke that pack's dataset preprocessor), it uses
[load_pack_data_module] to load the `.py` file dynamically.

This module exists so the loader lives in exactly one place; three
divergent copies of the same helper — one per script that needed it —
was the shape that let the sys.modules-registration bug regress.
"""

from __future__ import annotations

import importlib.util
import sys
from pathlib import Path
from types import ModuleType


def load_pack_data_module(pack_root: Path, module_name: str) -> ModuleType:
    """Load `packs/{pack_code}/data/{module_name}.py` via importlib.

    Registers the module in `sys.modules` BEFORE `exec_module`. Any
    `@dataclass` (or other decorator that reflects on
    `sys.modules[cls.__module__]`) defined inside the loaded file is
    evaluated at class-creation time — during `exec_module`. If the
    module isn't yet in `sys.modules` the lookup returns `None` and
    dataclasses.py raises

        AttributeError: 'NoneType' object has no attribute '__dict__'

    …with a traceback that points at the `@dataclass` line rather than
    at anything obviously import-related. Registering first avoids that.

    Args:
        pack_root: absolute path to the pack directory (e.g. `ml/packs/ase`).
        module_name: file stem without `.py` (e.g. `asl_alphabet`).

    Returns:
        The loaded module object. Symbols on it are accessed normally
        (`asl_module.build_split(...)`).

    Raises:
        SystemExit: if the pack has no such data module. Kept as
            SystemExit (not FileNotFoundError) so CLI callers get a clean
            top-level message instead of a stack trace.
    """
    path = pack_root / "data" / f"{module_name}.py"
    if not path.exists():
        raise SystemExit(
            f"Pack '{pack_root.name}' has no data loader '{module_name}' at {path}."
        )
    module_qualname = f"_pack_{pack_root.name}_{module_name}"
    spec = importlib.util.spec_from_file_location(module_qualname, path)
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    # sys.modules registration BEFORE exec_module — see docstring.
    sys.modules[module_qualname] = module
    try:
        spec.loader.exec_module(module)
    except BaseException:
        # A failing load shouldn't leave a half-initialized module in
        # sys.modules — subsequent retries would see the broken state.
        sys.modules.pop(module_qualname, None)
        raise
    return module
