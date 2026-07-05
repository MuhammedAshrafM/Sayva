"""PyTorch LSTM for the 5-sign toy vocabulary (Track C).

Architecture:
    Input(T, 84)
      → LSTM(input=84, hidden=64, num_layers=2, dropout=0.3, batch_first=True)
      → last-timestep hidden state → Linear(64, num_classes)

Design choices:
  * Two stacked LSTM layers with dropout between them — standard baseline.
    Enough capacity for 5 classes without overfitting a small dataset.
  * We take the LSTM's `output` at the last timestep, not the final hidden
    state, so temporal padding on shorter clips doesn't corrupt the encoding
    (the last non-pad frame gets consumed by the LSTM regardless).
  * `batch_first=True` for parity with `[batch, time, features]` conventions
    on both PyTorch and TFLite.

Fixed sequence length: sequences are resampled/padded to a constant T on the
preprocessing side (`preprocess_two_hand_sequence`), so the LSTM sees a static
shape — required for a clean TFLite export.
"""

from __future__ import annotations

import torch
from torch import nn

INPUT_FEATURES = 84  # 21 landmarks × 2 coords × 2 hands
DEFAULT_SEQUENCE_LENGTH = 30
DEFAULT_HIDDEN_SIZE = 64
DEFAULT_NUM_LAYERS = 2
DEFAULT_DROPOUT = 0.3


class TemporalSignLstm(nn.Module):
    def __init__(
        self,
        num_classes: int,
        hidden_size: int = DEFAULT_HIDDEN_SIZE,
        num_layers: int = DEFAULT_NUM_LAYERS,
        dropout: float = DEFAULT_DROPOUT,
    ) -> None:
        super().__init__()
        self.num_classes = num_classes
        self.hidden_size = hidden_size
        self.num_layers = num_layers
        self.lstm = nn.LSTM(
            input_size=INPUT_FEATURES,
            hidden_size=hidden_size,
            num_layers=num_layers,
            dropout=dropout if num_layers > 1 else 0.0,
            batch_first=True,
        )
        self.head = nn.Linear(hidden_size, num_classes)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        """
        Args:
            x: shape `[batch, T, 84]`.

        Returns:
            Logits, shape `[batch, num_classes]`.
        """
        # LSTM outputs (output, (h, c)) — take output at last timestep.
        output, _ = self.lstm(x)
        last = output[:, -1, :]
        return self.head(last)

    @torch.inference_mode()
    def predict_proba(self, x: torch.Tensor) -> torch.Tensor:
        was_training = self.training
        self.eval()
        try:
            return torch.softmax(self.forward(x), dim=-1)
        finally:
            if was_training:
                self.train()


def parameter_count(model: nn.Module) -> int:
    return sum(p.numel() for p in model.parameters() if p.requires_grad)
