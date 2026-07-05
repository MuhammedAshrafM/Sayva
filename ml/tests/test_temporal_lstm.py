"""Shape/param/behavior checks for the temporal LSTM.

Full training is exercised end-to-end by `train_temporal.py --data synthetic`
in the same way `train_fingerspelling.py` covers the MLP path.
"""

from __future__ import annotations

import torch

from sayva_ml.models.temporal_lstm import (
    DEFAULT_HIDDEN_SIZE,
    DEFAULT_NUM_LAYERS,
    INPUT_FEATURES,
    TemporalSignLstm,
    parameter_count,
)


def test_forward_output_shape() -> None:
    model = TemporalSignLstm(num_classes=5)
    x = torch.zeros(3, 30, INPUT_FEATURES)
    out = model(x)
    assert out.shape == (3, 5)


def test_predict_proba_sums_to_one() -> None:
    model = TemporalSignLstm(num_classes=5)
    x = torch.randn(4, 30, INPUT_FEATURES)
    p = model.predict_proba(x)
    assert torch.allclose(p.sum(dim=-1), torch.ones(4), atol=1e-6)


def test_variable_batch_and_sequence_length() -> None:
    """LSTM should work for any batch size and any T — export path pins T=30
    only when writing to ONNX/TFLite."""
    model = TemporalSignLstm(num_classes=5)
    for batch in (1, 8):
        for t in (10, 30, 60):
            out = model(torch.zeros(batch, t, INPUT_FEATURES))
            assert out.shape == (batch, 5)


def test_parameter_count_matches_architecture() -> None:
    """LSTM(84 → 64, 2 layers, dropout) + Linear(64, 5). If this changes,
    audit whether ONNX/TFLite export still succeeds."""
    model = TemporalSignLstm(num_classes=5)
    count = parameter_count(model)
    # Sanity: 5–100 K params — an LSTM with 84→64→64 has ~65k params.
    assert 30_000 < count < 200_000, f"LSTM param count {count} outside sane range"
    assert DEFAULT_HIDDEN_SIZE == 64
    assert DEFAULT_NUM_LAYERS == 2
