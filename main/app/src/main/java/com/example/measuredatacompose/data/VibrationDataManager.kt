package com.example.measuredatacompose.data

import android.util.Log
import com.example.measuredatacompose.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

class VibrationDataManager(private val directory: File) {
    private val vibrationInfoFile = File(directory, "vibrationExposure.txt")
    
    var totalDailyVibrationDuration: Duration = Duration.ZERO
        private set

    private var lastVibrationTimestamp: Instant? = null

    init {
        // Initial load can be done here or explicitly called
    }

    suspend fun reloadVibrationData() = withContext(Dispatchers.IO) {
        if (vibrationInfoFile.exists()) {
            try {
                val content = vibrationInfoFile.readText().trim()
                if (content.isNotEmpty()) {
                    totalDailyVibrationDuration = Duration.parse(content)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Can't reload vibration data", e)
            }
        }
    }

    fun trackVibration(vibrationData: VibrationData, threshold: Double) {
        if (vibrationData.magnitude >= threshold) {
            if (lastVibrationTimestamp != null) {
                val duration = Duration.between(lastVibrationTimestamp, vibrationData.timestamp)
                // If the gap is too large (e.g., > 1 second), don't count it as continuous vibration
                if (duration <= Duration.ofSeconds(1)) {
                    totalDailyVibrationDuration = totalDailyVibrationDuration.plus(duration)
                }
            }
            lastVibrationTimestamp = vibrationData.timestamp
        } else {
            lastVibrationTimestamp = null
        }

        saveVibrationData()
    }

    private fun saveVibrationData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                vibrationInfoFile.writeText(totalDailyVibrationDuration.toString())
            } catch (e: Exception) {
                Log.e(TAG, "Can't write vibration exposure", e)
            }
        }
    }

    fun resetDailyVibration() {
        totalDailyVibrationDuration = Duration.ZERO
        saveVibrationData()
    }
}
