"""Shape/param checks for the fingerspelling MLP.

We do NOT run a mini training loop here — that's covered by the end-to-end
`train_fingerspelling.py --data synthetic` smoke path invoked from CI.
"""

from __future__ import annotations

import torch

from sayva_ml.models.fingerspelling_mlp import FingerspellingMLP, parameter_count


def test_forward_output_shape_matches_num_classes() -> None:
    model = FingerspellingMLP(num_classes=24)
    x = torch.zeros(3, 42)
    out = model(x)
    assert out.shape == (3, 24)


def test_predict_proba_sums_to_one() -> None:
    model = FingerspellingMLP(num_classes=24)
    x = torch.randn(5, 42)
    probs = model.predict_proba(x)
    row_sums = probs.sum(dim=-1)
    assert torch.allclose(row_sums, torch.ones(5), atol=1e-6), row_sums


def test_parameter_count_is_tiny() -> None:
    """Kazuhito's architecture is intentionally minimal — verify we haven't
    accidentally bloated it. 24-class version is 1,334 params."""
    model = FingerspellingMLP(num_classes=24)
    count = parameter_count(model)
    # 42*20 + 20 + 20*10 + 10 + 10*24 + 24 = 840 + 20 + 200 + 10 + 240 + 24 = 1334
    assert count == 1334, f"MLP parameter count changed to {count} — architecture drift?"


def test_dropout_is_active_in_train_mode() -> None:
    """Two forward passes with dropout should differ; eval mode should be deterministic."""
    torch.manual_seed(0)
    model = FingerspellingMLP(num_classes=24)
    x = torch.randn(1, 42)
    model.train()
    a = model(x)
    b = model(x)
    assert not torch.allclose(a, b), "Dropout should perturb train-mode forward"
    model.eval()
    c = model(x)
    d = model(x)
    assert torch.allclose(c, d), "Eval mode must be deterministic"
