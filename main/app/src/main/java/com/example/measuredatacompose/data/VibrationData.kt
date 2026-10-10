package com.example.measuredatacompose.data

import java.time.Instant

/**
 * Represents a vibration measurement from the accelerometer.
 *
 * @param magnitude The magnitude of the acceleration vector (m/s²).
 * @param timestamp The time at which the measurement was taken.
 */
data class VibrationData(
    val magnitude: Double,
    val timestamp: Instant
)