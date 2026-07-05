"""Synthetic temporal sign dataset — obviously-separable per-class trajectories.

Same purpose as `sayva_ml.data.synthetic`: prove the whole train → export →
consumption pipeline works without needing WLASL clips downloaded and
processed through MediaPipe. Every class gets:

  * A deterministic per-class "prototype trajectory" — one (left, right)
    hand pose per timestep, evolving linearly from a starting pose.
  * Gaussian jitter around it per sample so the model must generalize.

Real WLASL preprocessing lives in `data/wlasl.py` (skeleton — needs the user
to download clips first). This file's output has zero predictive value for
real signs; it exists to give the LSTM a shape and label set to fit against.
"""

from __future__ import annotations

import random
from dataclasses import dataclass

import numpy as np

from sayva_ml.preprocessing.landmark import pre_process_two_hand_sequence


@dataclass(frozen=True)
class TemporalSplit:
    X_train: np.ndarray  # shape (N_train, T, 84)
    y_train: np.ndarray  # shape (N_train,)
    X_val: np.ndarray
    y_val: np.ndarray
    X_test: np.ndarray
    y_test: np.ndarray


def _prototype_pose(class_index: int, hand_side: int, seed: int) -> list[list[float]]:
    """21 landmarks for one hand at time-0 of one class.

    `hand_side=0` is left, `1` is right. Seed is different per (class, hand)
    so each class has a distinct starting pose for each hand.
    """
    rng = random.Random(1000 + class_index * 17 + hand_side * 3 + seed)
    return [[rng.uniform(200, 500), rng.uniform(150, 400)] for _ in range(21)]


def _prototype_velocity(class_index: int, hand_side: int, seed: int) -> tuple[float, float]:
    """Constant per-frame drift for one hand of one class (dx, dy pixels)."""
    rng = random.Random(9000 + class_index * 13 + hand_side * 5 + seed)
    return rng.uniform(-6.0, 6.0), rng.uniform(-6.0, 6.0)


def _sample_trajectory(
    class_index: int,
    sequence_length: int,
    hand_side: int,
    rng: random.Random,
    seed: int,
    pose_noise: float,
    velocity_noise: float,
) -> list[list[list[float]]]:
    """One (T, 21, 2) trajectory for one hand of one sample."""
    proto = _prototype_pose(class_index, hand_side, seed)
    dx, dy = _prototype_velocity(class_index, hand_side, seed)
    dx_n = dx + rng.gauss(0.0, velocity_noise)
    dy_n = dy + rng.gauss(0.0, velocity_noise)
    frames: list[list[list[float]]] = []
    for t in range(sequence_length):
        offset_x = dx_n * t
        offset_y = dy_n * t
        pose = [
            [
                p[0] + offset_x + rng.gauss(0.0, pose_noise),
                p[1] + offset_y + rng.gauss(0.0, pose_noise),
            ]
            for p in proto
        ]
        frames.append(pose)
    return frames


def make_split(
    num_classes: int,
    samples_per_class: int,
    sequence_length: int = 30,
    train_ratio: float = 0.75,
    val_ratio: float = 0.125,
    pose_noise: float = 8.0,
    velocity_noise: float = 1.5,
    seed: int = 42,
) -> TemporalSplit:
    """Build a labeled temporal dataset and split into train/val/test.

    Both hands are always present in the synthetic case — simulating a
    well-detected two-handed sign. Real WLASL preprocessing handles missing
    hands via the same zero-fill path exercised in the parity test suite.
    """
    rng = random.Random(seed)

    all_X: list[list[list[float]]] = []  # each entry: (T, 84)
    all_y: list[int] = []
    for class_idx in range(num_classes):
        for _ in range(samples_per_class):
            left_traj = _sample_trajectory(
                class_idx, sequence_length, hand_side=0, rng=rng,
                seed=seed, pose_noise=pose_noise, velocity_noise=velocity_noise,
            )
            right_traj = _sample_trajectory(
                class_idx, sequence_length, hand_side=1, rng=rng,
                seed=seed, pose_noise=pose_noise, velocity_noise=velocity_noise,
            )
            # `pre_process_two_hand_sequence` handles the (T, 84) shape
            # including the normalization.
            frames = list(zip(left_traj, right_traj, strict=True))
            processed = pre_process_two_hand_sequence(frames, target_length=sequence_length)
            all_X.append(processed)
            all_y.append(class_idx)

    indices = list(range(len(all_X)))
    rng.shuffle(indices)
    X = np.asarray([all_X[i] for i in indices], dtype=np.float32)
    y = np.asarray([all_y[i] for i in indices], dtype=np.int64)

    n = len(X)
    n_train = int(n * train_ratio)
    n_val = int(n * val_ratio)
    return TemporalSplit(
        X_train=X[:n_train],
        y_train=y[:n_train],
        X_val=X[n_train : n_train + n_val],
        y_val=y[n_train : n_train + n_val],
        X_test=X[n_train + n_val :],
        y_test=y[n_train + n_val :],
    )
