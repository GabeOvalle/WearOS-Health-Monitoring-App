package com.example.measuredatacompose.presentation

import java.time.Duration
import java.util.Locale

/**
 * Formats a [Duration] into a clock format string.
 * Negative durations are prefixed with a "-" sign.
 */
fun Duration.formatClock(): String {
    val totalSeconds = seconds
    val sign = if (totalSeconds < 0) "-" else ""
    val absSeconds = kotlin.math.abs(totalSeconds)
    val hours = absSeconds / 3600
    val minutes = (absSeconds % 3600) / 60
    val secs = absSeconds % 60

    return if (hours > 0) {
        sign + String.format(Locale.US, "%d:%02d:%02d", hours, minutes, secs)
    } else {
        sign + String.format(Locale.US, "%d:%02d", minutes, secs)
    }
}

