package com.example.measuredatacompose.data

import androidx.health.services.client.data.DataTypeAvailability
import java.time.Instant

/**
 * Represents a single heart rate measurement.
 *
 * This domain object is used to decouple the rest of the app from the Health Services API's
 * `SampleDataPoint` class.
 *
 * @param value The heart rate in beats per minute (BPM).
 * @param timestamp The time at which the measurement was taken.
 * @param availability The availability of the sensor at the time of the measurement.
 * @param isAccurate Whether the sensor is capable of providing a more accurate measurement.
 * @param canRecord Whether the system should record this value or not.
 */
data class Bpm(
    val value: Double?,
    val timestamp: Instant,
    val availability: DataTypeAvailability,
    val isAccurate: Boolean,
    val canRecord: Boolean
)