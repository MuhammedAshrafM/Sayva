"""PyTorch reimplementation of Kazuhito's keypoint classifier MLP.

Architecture (from `keypoint_classification.ipynb` cell 10):
    Input(42)
      → Dropout(0.2)
      → Dense(20, relu)
      → Dropout(0.4)
      → Dense(10, relu)
      → Dense(N, softmax)

Weights are trained from scratch — no weights are transferred from Kazuhito.
His shipped `.tflite` is a 3-class classifier (Open/Close/Pointer); ours is
24-class (ASL alphabet minus J and Z per `ml/configs/fingerspelling.yaml`).
"""

from __future__ import annotations

import torch
from torch import nn

INPUT_SIZE = 42


class FingerspellingMLP(nn.Module):
    """Kazuhito's Keras MLP, mapped 1:1 to PyTorch.

    Note: `softmax` is applied inside `forward` at inference time only. At
    training time we return raw logits and use `CrossEntropyLoss`, which
    combines log-softmax + NLL — the numerically-stable path.
    """

    def __init__(self, num_classes: int) -> None:
        super().__init__()
        self.num_classes = num_classes
        # Sequential in Keras terms:
        # Dropout(0.2) — Dense(20) relu — Dropout(0.4) — Dense(10) relu — Dense(N).
        self.stack = nn.Sequential(
            nn.Dropout(0.2),
            nn.Linear(INPUT_SIZE, 20),
            nn.ReLU(),
            nn.Dropout(0.4),
            nn.Linear(20, 10),
            nn.ReLU(),
            nn.Linear(10, num_classes),
        )

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        """Return raw logits. Wrap the caller in `torch.softmax` if probabilities are needed."""
        return self.stack(x)

    @torch.inference_mode()
    def predict_proba(self, x: torch.Tensor) -> torch.Tensor:
        """Convenience: return softmax probabilities. Uses eval mode."""
        was_training = self.training
        self.eval()
        try:
            logits = self.forward(x)
            return torch.softmax(logits, dim=-1)
        finally:
            if was_training:
                self.train()


def parameter_count(model: nn.Module) -> int:
    return sum(p.numel() for p in model.parameters() if p.requires_grad)
