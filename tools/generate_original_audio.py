#!/usr/bin/env python3
"""Deterministically synthesize the Health FX Lab's original mono source WAV files."""

from __future__ import annotations

import argparse
import math
import random
import wave
from array import array
from pathlib import Path

SAMPLE_RATE = 48_000
TAU = math.tau


def silence(seconds: float) -> list[float]:
    return [0.0] * round(seconds * SAMPLE_RATE)


def add_tone(
    data: list[float],
    start: float,
    duration: float,
    frequency: float,
    gain: float,
    attack: float = 0.004,
    decay_power: float = 2.0,
    end_frequency: float | None = None,
) -> None:
    begin = round(start * SAMPLE_RATE)
    count = min(round(duration * SAMPLE_RATE), len(data) - begin)
    phase = 0.0
    for index in range(max(0, count)):
        elapsed = index / SAMPLE_RATE
        progress = index / max(1, count - 1)
        frequency_at_sample = frequency + ((end_frequency or frequency) - frequency) * progress
        phase += TAU * frequency_at_sample / SAMPLE_RATE
        attack_gain = min(1.0, elapsed / max(attack, 1.0 / SAMPLE_RATE))
        envelope = attack_gain * (1.0 - progress) ** decay_power
        data[begin + index] += math.sin(phase) * gain * envelope


def filtered_noise(
    rng: random.Random,
    count: int,
    cutoff_hz: float,
    high_pass: bool = False,
) -> list[float]:
    alpha = math.exp(-TAU * cutoff_hz / SAMPLE_RATE)
    low = 0.0
    result: list[float] = []
    for _ in range(count):
        raw = rng.uniform(-1.0, 1.0)
        low = alpha * low + (1.0 - alpha) * raw
        result.append(raw - low if high_pass else low)
    return result


def add_noise_burst(
    data: list[float],
    rng: random.Random,
    start: float,
    duration: float,
    gain: float,
    cutoff_hz: float,
    attack: float = 0.002,
    decay_power: float = 2.5,
    high_pass: bool = False,
) -> None:
    begin = round(start * SAMPLE_RATE)
    count = min(round(duration * SAMPLE_RATE), len(data) - begin)
    noise = filtered_noise(rng, max(0, count), cutoff_hz, high_pass)
    for index, sample in enumerate(noise):
        elapsed = index / SAMPLE_RATE
        progress = index / max(1, count - 1)
        envelope = min(1.0, elapsed / max(attack, 1.0 / SAMPLE_RATE))
        envelope *= (1.0 - progress) ** decay_power
        data[begin + index] += sample * gain * envelope


def add_breath(data: list[float], rng: random.Random) -> None:
    noise = filtered_noise(rng, len(data), 1350.0, high_pass=True)
    low = filtered_noise(rng, len(data), 420.0, high_pass=False)
    for index in range(len(data)):
        t = index / SAMPLE_RATE
        progress = index / max(1, len(data) - 1)
        inhale = math.sin(math.pi * min(1.0, progress / 0.45)) ** 2 if progress < 0.45 else 0.0
        exhale_progress = max(0.0, (progress - 0.42) / 0.58)
        exhale = math.sin(math.pi * min(1.0, exhale_progress)) ** 1.6
        envelope = 0.24 * inhale + 0.54 * exhale
        tremor = 0.90 + 0.10 * math.sin(TAU * 5.1 * t)
        data[index] += (noise[index] * 0.24 + low[index] * 0.16) * envelope * tremor


def normalize(data: list[float], peak: float = 0.50) -> list[float]:
    actual = max((abs(sample) for sample in data), default=1.0)
    scale = peak / actual if actual > peak else 1.0
    return [max(-1.0, min(1.0, sample * scale)) for sample in data]


def write_wav(path: Path, data: list[float]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    pcm = array("h", (round(sample * 32767.0) for sample in normalize(data)))
    with wave.open(str(path), "wb") as output:
        output.setnchannels(1)
        output.setsampwidth(2)
        output.setframerate(SAMPLE_RATE)
        output.writeframes(pcm.tobytes())


def bleed_pulse() -> list[float]:
    rng = random.Random(0xB1EED)
    data = silence(0.95)
    add_tone(data, 0.04, 0.28, 58.0, 0.90, decay_power=3.2, end_frequency=42.0)
    add_tone(data, 0.30, 0.24, 52.0, 0.60, decay_power=3.8, end_frequency=38.0)
    add_noise_burst(data, rng, 0.02, 0.50, 0.48, 280.0, decay_power=2.8)
    return data


def fracture_onset() -> list[float]:
    rng = random.Random(0xF2AC7)
    data = silence(0.62)
    for start, gain in ((0.035, 0.95), (0.071, 0.62), (0.114, 0.42)):
        add_noise_burst(data, rng, start, 0.075, gain, 2100.0, high_pass=True, decay_power=4.8)
    add_tone(data, 0.02, 0.34, 92.0, 0.56, decay_power=3.4, end_frequency=52.0)
    add_noise_burst(data, rng, 0.11, 0.36, 0.32, 360.0, decay_power=3.0)
    return data


def fracture_step() -> list[float]:
    rng = random.Random(0x57E9)
    data = silence(0.46)
    add_tone(data, 0.02, 0.24, 78.0, 0.42, decay_power=3.4, end_frequency=52.0)
    add_noise_burst(data, rng, 0.025, 0.31, 0.38, 680.0, decay_power=3.6)
    add_noise_burst(data, rng, 0.045, 0.12, 0.24, 1800.0, high_pass=True, decay_power=5.0)
    return data


def pain_sting() -> list[float]:
    rng = random.Random(0x9A1)
    data = silence(1.10)
    add_tone(data, 0.015, 0.34, 66.0, 0.78, decay_power=3.6, end_frequency=45.0)
    add_tone(data, 0.03, 0.92, 1480.0, 0.18, attack=0.008, decay_power=2.3, end_frequency=1120.0)
    add_noise_burst(data, rng, 0.02, 0.28, 0.40, 1600.0, high_pass=True, decay_power=4.2)
    return data


def pain_breath() -> list[float]:
    rng = random.Random(0xB2EA7)
    data = silence(1.75)
    add_breath(data, rng)
    add_tone(data, 0.82, 0.66, 138.0, 0.055, attack=0.12, decay_power=1.8, end_frequency=112.0)
    return data


def relief() -> list[float]:
    rng = random.Random(0x2E11EF)
    data = silence(0.86)
    add_noise_burst(data, rng, 0.03, 0.55, 0.30, 520.0, attack=0.025, decay_power=2.4)
    add_tone(data, 0.17, 0.30, 420.0, 0.14, attack=0.012, decay_power=3.0, end_frequency=310.0)
    add_tone(data, 0.42, 0.24, 315.0, 0.10, attack=0.012, decay_power=3.0, end_frequency=240.0)
    return data


GENERATORS = {
    "bleed_pulse": bleed_pulse,
    "fracture_onset": fracture_onset,
    "fracture_step": fracture_step,
    "pain_sting": pain_sting,
    "pain_breath": pain_breath,
    "relief": relief,
}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--out-dir", type=Path, required=True)
    args = parser.parse_args()
    for name, generator in GENERATORS.items():
        target = args.out_dir / f"{name}.wav"
        write_wav(target, generator())
        print(target)


if __name__ == "__main__":
    main()
