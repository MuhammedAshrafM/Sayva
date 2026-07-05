#!/bin/bash
set -euo pipefail
export PATH="$HOME/.local/bin:$PATH"
export UV_PROJECT_ENVIRONMENT=/root/sayva-ml-venv
cd /mnt/d/Projects/KMP/Sayva/ml
uv sync --extra export
