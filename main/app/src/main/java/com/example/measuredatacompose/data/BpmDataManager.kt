package com.example.measuredatacompose.data

import android.util.Log
import androidx.health.services.client.data.DataTypeAvailability
import com.example.measuredatacompose.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class BpmDataManager(private val directory: File,
                     private val healthServicesRepository: HealthServicesRepository
) {
    val dailyBPMAverages = File(directory, "dailyBPMAverages.bin")

    private var previous: Bpm? = null
    var totalDailyHIDuration = Duration.ZERO
    var burstHIDuration = Duration.ZERO
    var lastHIState = System.currentTimeMillis()
    private var dailyBPMSum = 0.0
    private var dailySampleCount = 0
    var dailyAverage: Double? = null

    fun writeHeartRateData(heartRateBpm: List<Bpm>) {

        CoroutineScope(Dispatchers.IO).launch {

            val todayFile = File(directory, "${LocalDate.now()}.bin")
            if (!todayFile.exists()) {
                prepareForNewDay()
            }

            try {
                // Each BPM object's value, timestamp, and isAccurate boolean are written to a binary file.
                DataOutputStream(BufferedOutputStream(FileOutputStream(todayFile, true))).use { output ->
                    heartRateBpm.forEach { bpm ->
                        if (bpm.value != null) {
                            output.writeDouble(bpm.value)
                            output.writeLong(bpm.timestamp.toEpochMilli())
                            output.writeBoolean(bpm.isAccurate)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "File write error", e)
            }
        }
    }

    //Changed the way the average is calculated to avoid constant file reading
    fun trackDailyBPMAverage(heartRateBPM: List<Bpm>) {
        heartRateBPM.forEach { current ->
            if (current.isAccurate && current.value != null) {
                dailyBPMSum += current.value
                dailySampleCount++
                dailyAverage = dailyBPMSum / dailySampleCount
            }
        }
        if (dailyAverage != null) {
            CoroutineScope(Dispatchers.IO).launch {
                val dailyRunningCount = File(directory, "dailyRunningCount.bin")
                try {
                    DataOutputStream(BufferedOutputStream(FileOutputStream(dailyRunningCount))).use {
                        it.writeDouble(dailyBPMSum)
                        it.writeInt(dailySampleCount)
                        it.writeDouble(dailyAverage!!)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Can't save daily running count", e)
                }
            }
        }
    }

    //Made this a suspend function to prevent ANRs and UI freezes
    suspend fun getHeartRateData(bpmData: File = File(directory, "${LocalDate.now()}.bin")): List<Bpm> = withContext(Dispatchers.IO) {
        val heartRateBPM = mutableListOf<Bpm>()

        if (bpmData.exists()) {
            try {
                DataInputStream(BufferedInputStream(FileInputStream(bpmData))).use { input ->
                    while (true) {
                        try {
                            val value = input.readDouble()
                            val timestamp = input.readLong()
                            val isAccurate = input.readBoolean()

                            heartRateBPM.add(Bpm(value, Instant.ofEpochMilli(timestamp), DataTypeAvailability.AVAILABLE, isAccurate, true))
                        } catch (e: EOFException) {
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "File read error", e)
            }
        }

        heartRateBPM
    }

    //Fixed a logic error in this function so that bursts are tracked accurately
    //Put tracking and saving high impact bpmData into the same function
    fun trackHighImpactActivity(heartRateBPM: List<Bpm>) {
        heartRateBPM.forEach { current ->
            if (previous != null) {
                val duration = Duration.between(previous!!.timestamp, current.timestamp)
                val isCurrentHigh = current.isAccurate && current.value != null && current.value!! >= healthServicesRepository.alertUpperBound*.7
                val isPrevHigh = previous!!.isAccurate && previous!!.value != null && previous!!.value!! >= healthServicesRepository.alertUpperBound*.7

                if (isPrevHigh && isCurrentHigh && duration <= Duration.ofSeconds(5)) {
                    totalDailyHIDuration += duration
                    burstHIDuration += duration
                    lastHIState = System.currentTimeMillis()
                } else if (isCurrentHigh) {
                    burstHIDuration = Duration.ZERO
                }
            }
            previous = current
        }

        CoroutineScope(Dispatchers.IO).launch {
            val highImpactInfo = File(directory, "highImpactInfo.txt")
            try {
                highImpactInfo.writeText("$totalDailyHIDuration $burstHIDuration")
            } catch (e: Exception) {
                Log.e(TAG, "Can't write HI activity", e)
            }
        }
    }

    suspend fun reloadDailyRunningCount() = withContext(Dispatchers.IO) {
        val dailyRunningCount = File(directory, "dailyRunningCount.bin")
        try {
            if (dailyRunningCount.exists()) {
                DataInputStream(BufferedInputStream(FileInputStream(dailyRunningCount))).use {
                    val sum = it.readDouble()
                    val count = it.readInt()
                    val average = it.readDouble()
                    dailyBPMSum = sum
                    dailySampleCount = count
                    dailyAverage = average
                }
            } else {

            }
        } catch (e: EOFException) {

        } catch (e: Exception) {
            Log.e(TAG, "Can't reload daily running count", e)
        }
    }

    //Made this a suspend function to prevent ANRs and UI freezes
    suspend fun reloadHighImpactInfo() = withContext(Dispatchers.IO) {
        val highImpactInfo = File(directory, "highImpactInfo.txt")
        if (highImpactInfo.exists()) {
            try {
                val lines = highImpactInfo.readLines()
                if (lines.isNotEmpty()) {
                    val parts = lines[0].trim().split(Regex("\\s+"))
                    if (parts.size >= 2) {
                        val daily = Duration.parse(parts[0])
                        val burst = Duration.parse(parts[1])
                        totalDailyHIDuration = daily
                        burstHIDuration = burst
                    }
                }
            } catch (e: EOFException) {

            } catch (e: Exception) {
                Log.e(TAG, "Can't reload HI activity", e)
            }
        }
    }

    //Made the daily reset algorithm inside writeHeartRateData its own function to improve modularity
    private fun prepareForNewDay() {
        totalDailyHIDuration = Duration.ZERO

        if (isFileOneYearOld(dailyBPMAverages)) {
            dailyBPMAverages.delete()
        }

        val lastDay = getMostRecentPreviousFile(directory)

        if (lastDay != null) {
            if (dailyAverage != null) {
                try {
                    DataOutputStream(BufferedOutputStream(FileOutputStream(dailyBPMAverages, true))).use {
                        it.writeDouble(dailyAverage!!)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Can't save daily average", e)
                }
            }

            dailyBPMSum = 0.0
            dailySampleCount = 0
            dailyAverage = null

            // The previous day's file is deleted when a new day's file is created.
            lastDay.delete()
        }
    }

    //Fixed this function so that file age is determined based on creation time instead of modification time
    private fun isFileOneYearOld(file: File): Boolean {
        if (!file.exists()) return false

        try {
            val attributes = Files.readAttributes(file.toPath(), java.nio.file.attribute.BasicFileAttributes::class.java)
            val creationInstant = Instant.ofEpochMilli(attributes.creationTime().toMillis())
            val oneYearAgo = Instant.now().minus(365, ChronoUnit.DAYS)

            return creationInstant.isBefore(oneYearAgo)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking file age", e)
            return false
        }
    }

    private fun getMostRecentPreviousFile(directory: File): File? {
        val today = LocalDate.now()
        val dateRegex = Regex("\\d{4}-\\d{2}-\\d{2}")

        return directory.listFiles()
            ?.filter { file ->
                dateRegex.matches(file.nameWithoutExtension)
            }
            ?.mapNotNull { file ->
                val date = LocalDate.parse(file.nameWithoutExtension)
                if (date.isBefore(today)) Pair(date, file) else null
            }
            ?.maxByOrNull { it.first }
            ?.second
    }
}

