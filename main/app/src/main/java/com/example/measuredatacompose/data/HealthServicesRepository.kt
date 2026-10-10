/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this dailyBPMData except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.measuredatacompose.data

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.concurrent.futures.await
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import androidx.health.services.client.data.DeltaDataType
import com.example.measuredatacompose.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.sqrt

/**
 * Entry point for [HealthServicesClient] APIs. This also provides suspend functions around
 * those APIs to enable use in coroutines.
 */
class HealthServicesRepository(
    private val context: Context,
    private val stepTracker: StepPassiveTracker
) {
    private val healthServicesClient = HealthServices.getClient(context)
    private val measureClient = healthServicesClient.measureClient
    private val prefs: SharedPreferences = context.getSharedPreferences("health_prefs", Context.MODE_PRIVATE)

    val bpmDataManager = BpmDataManager(context.filesDir, this)
    val vibrationDataManager = VibrationDataManager(context.filesDir)

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val goalTargetsFile = java.io.File(context.filesDir, "goalTargets.txt")

    private val _alerts = MutableSharedFlow<String>(replay = 1)
    val alerts: SharedFlow<String> = _alerts.asSharedFlow()
    private val alertQueue = ConcurrentLinkedQueue<String>()

    private val _highImpactState = MutableStateFlow(HighImpactState())
    val highImpactState: StateFlow<HighImpactState> = _highImpactState.asStateFlow()

    private var lastSafetyAlertTime: Instant = Instant.EPOCH
    private var highImpactAlertSent = false

    private val _hourlyUi = MutableStateFlow("No data available")
    val hourlyUi: StateFlow<String> = _hourlyUi.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val vibrationThreshold = 0.8

    // Persistent alert bounds
    var alertUpperBound: Double
        get() = prefs.getFloat("upper_bound", 180.0f).toDouble()
        set(value) { prefs.edit().putFloat("upper_bound", value.toFloat()).apply() }

    var alertLowerBound: Double
        get() = prefs.getFloat("lower_bound", 60.0f).toDouble()
        set(value) { prefs.edit().putFloat("lower_bound", value.toFloat()).apply() }
    fun setServiceRunning(running: Boolean) {
        _isServiceRunning.value = running
    }

    init {
        repositoryScope.launch {
            bpmDataManager.reloadDailyRunningCount()
            bpmDataManager.reloadHighImpactInfo()
            vibrationDataManager.reloadVibrationData()
            _highImpactState.value = HighImpactState(
                totalDailyDuration = bpmDataManager.totalDailyHIDuration,
                burstDuration = bpmDataManager.burstHIDuration,
                vibrationExposure = vibrationDataManager.totalDailyVibrationDuration
            )
        }
        repositoryScope.launch(Dispatchers.IO) {
            refreshHourlyUi()
        }
    }

    suspend fun hasHeartRateCapability(): Boolean {
        val capabilities = measureClient.getCapabilitiesAsync().await()
        return (DataType.HEART_RATE_BPM in capabilities.supportedDataTypesMeasure)
    }


    // Switched to TYPE_NOTIFICATION for a shorter "chime" sound
    private val alertSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    private val mp: MediaPlayer? = MediaPlayer.create(context, alertSound)

    /**
     * Returns a cold flow of vibration data from the accelerometer.
     */
    val vibrationFlow: Flow<VibrationData> = callbackFlow {
        Log.i("VIBRATION_DEBUG", "VibrationFlow: callbackFlow STARTED")

        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        if (sensor == null) {
            Log.e("VIBRATION_DEBUG", "VibrationFlow: Linear acceleration sensor NOT AVAILABLE")
            close()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val magnitude = sqrt(
                        it.values[0] * it.values[0] +
                                it.values[1] * it.values[1] +
                                it.values[2] * it.values[2]
                    ).toDouble()
                    // Log raw magnitude for debugging
                    //Log.i("VIBRATION_DEBUG", "RAW VIBRATION: $magnitude")
                    trySendBlocking(VibrationData(magnitude, Instant.now()))
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        Log.i("VIBRATION_DEBUG", "VibrationFlow: Registering sensor listener (NORMAL)")
        val registered = sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        Log.i("VIBRATION_DEBUG", "VibrationFlow: Registration result = $registered")

        awaitClose {
            Log.i("VIBRATION_DEBUG", "VibrationFlow: callbackFlow CLOSING")
            sensorManager.unregisterListener(listener)
        }
    }
        .onEach { vibrationData ->
            if (vibrationData.magnitude >= vibrationThreshold) {
                Log.i("VIBRATION_DEBUG", "HIGH VIBRATION DETECTED: ${vibrationData.magnitude} m/s²")
            }
            vibrationDataManager.trackVibration(vibrationData, vibrationThreshold)
            _highImpactState.value = _highImpactState.value.copy(
                vibrationExposure = vibrationDataManager.totalDailyVibrationDuration
            )
        }
        .shareIn(
            scope = repositoryScope,
            started = SharingStarted.WhileSubscribed(0),
            replay = 1
        )

    /**
     * Returns a cold flow of [MeasureMessage] containing health data.
     */
    private val _measureFlow = callbackFlow {
        var currentHeartRateAvailability: DataTypeAvailability = DataTypeAvailability.UNKNOWN

        val callback = object : MeasureCallback {
            override fun onAvailabilityChanged(dataType: DeltaDataType<*, *>, availability: Availability) {
                if (availability is DataTypeAvailability) {
                    currentHeartRateAvailability = availability
                    trySendBlocking(MeasureMessage.MeasureAvailability(availability))
                }
            }

            override fun onDataReceived(data: DataPointContainer) {
                val bootTime = System.currentTimeMillis() - SystemClock.elapsedRealtime()
                val bpmEvents = data.getData(DataType.HEART_RATE_BPM).map {
                    Bpm(
                        value = it.value,
                        timestamp = Instant.ofEpochMilli(bootTime + it.timeDurationFromBoot.toMillis()),
                        availability = currentHeartRateAvailability,
                        isAccurate = currentHeartRateAvailability == DataTypeAvailability.AVAILABLE,
                        canRecord = true
                    )
                }

                if (bpmEvents.isNotEmpty()) {
                    trySendBlocking(MeasureMessage.MeasureData(bpmEvents))
                }
            }
        }

        measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, callback)
        stepTracker.start()
        awaitClose {
            runBlocking {
                measureClient.unregisterMeasureCallbackAsync(DataType.HEART_RATE_BPM, callback).await()
                stepTracker.stop()
                Log.d(TAG, "Unregistered callback")
            }
        }
    }
        .onEach { message ->
            if (message is MeasureMessage.MeasureData) {
                val bpmEvents = message.data

                if (bpmEvents.isNotEmpty()) {
                    bpmDataManager.writeHeartRateData(bpmEvents)
                    bpmDataManager.trackDailyBPMAverage(bpmEvents)
                    bpmDataManager.trackHighImpactActivity(bpmEvents)

                    _highImpactState.value = _highImpactState.value.copy(
                        totalDailyDuration = bpmDataManager.totalDailyHIDuration,
                        burstDuration = bpmDataManager.burstHIDuration
                    )
                    refreshHourlyUi()

                    val latestBpmPoint = bpmEvents.lastOrNull { it.isAccurate }
                    val latestBpm = latestBpmPoint?.value
                    val averageBpm = bpmDataManager.dailyAverage

                    if (latestBpm != null && averageBpm != null) {
                        // Publish to MQTT broker for manager portal integration
                        Messaging.sendHealthData(
                            heartRate = latestBpm,
                            timestamp = latestBpmPoint.timestamp.toEpochMilli(),
                            steps = stepTracker.stepCount.value,
                            readiness = calculateReadiness()
                        )

                        val now = Instant.now()
                        if (isHeartRateInAlertRange(latestBpm) && (Duration.between(lastSafetyAlertTime, now) > Duration.ofMinutes(2))) {
                            safetyAlert(latestBpm, averageBpm)
                            lastSafetyAlertTime = now
                        }
                    }

                    if (bpmDataManager.burstHIDuration >= Duration.ofSeconds(30)) {
                        if (!highImpactAlertSent) {
                            sendAlert("Unsafe Activity detected!")
                            triggerAlertSideEffects()
                            highImpactAlertSent = true
                        }
                    } else if (bpmDataManager.burstHIDuration == Duration.ZERO) {
                        highImpactAlertSent = false
                    }
                }
            }
        }
        .shareIn(repositoryScope, SharingStarted.WhileSubscribed(0), 1)

    fun measureFlow(): Flow<MeasureMessage> = _measureFlow

    fun isHeartRateInAlertRange(latest: Double): Boolean {
        return latest !in alertLowerBound..alertUpperBound
    }

    fun setAlertBounds(lower: Double, upper: Double) {
        alertLowerBound = lower
        alertUpperBound = upper
    }

    fun setAlertBounds(age: Int) {
        alertUpperBound = 220.0 - age
        alertLowerBound = if (age >= 13) 60.0 else 75.0
    }

    fun loadGoalTargets(): GoalTargets {
        if (!goalTargetsFile.exists()) return GoalTargets()
        return try {
            val parts = goalTargetsFile.readText().trim().split(Regex("\\s+"))
            GoalTargets(parts.getOrNull(0)?.toIntOrNull() ?: 10000, parts.getOrNull(1)?.toIntOrNull() ?: 30)
        } catch (e: Exception) { GoalTargets() }
    }

    fun saveGoalTargets(targets: GoalTargets) {
        try { goalTargetsFile.writeText("${targets.stepGoalTarget} ${targets.highImpactGoalMinutes}") } catch (e: Exception) {}
    }

    fun safetyAlert(latest: Double, average: Double) {
        val type = if (latest > average) "High" else "Low"
        sendAlert("$type HR Alert: $latest (Avg: ${average.toInt()})")
        triggerAlertSideEffects()
    }

    private fun sendAlert(message: String) {
        alertQueue.add(message)
        Messaging.sendAlert(message)
        repositoryScope.launch { _alerts.emit(message) }
    }

    private fun triggerAlertSideEffects() {
        val vibrator = context.getSystemService(Vibrator::class.java)
        vibrator?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        try { mp?.let { if (it.isPlaying) { it.pause(); it.seekTo(0) }; it.start(); repositoryScope.launch { delay(3000); stopAlertSound() } } } catch (e: Exception) {}
    }

    fun stopAlertSound() { try { if (mp?.isPlaying == true) { mp.pause(); mp.seekTo(0) } } catch (e: Exception) {} }

    private var refreshJob: Job? = null
    private fun refreshHourlyUi() {
        refreshJob?.cancel()
        refreshJob = repositoryScope.launch {
            val heartRateData = bpmDataManager.getHeartRateData()
            _hourlyUi.value = if (heartRateData.isEmpty()) "No data available" else HourlyDataManager(heartRateData.toMutableList()).toString()
        }
    }

    //Calculates the time since the last HI state and slowly increases from 0 to 100 over the course of 5 minutes since the last HI state
    private fun calculateReadiness(): Int{
        return ((System.currentTimeMillis() - bpmDataManager.lastHIState) / 3000).toInt()
            .coerceAtMost(100)
    }
}

data class HighImpactState(
    val totalDailyDuration: Duration = Duration.ZERO,
    val burstDuration: Duration = Duration.ZERO,
    val vibrationExposure: Duration = Duration.ZERO
)

sealed class MeasureMessage {
    class MeasureAvailability(val availability: DataTypeAvailability) : MeasureMessage()
    class MeasureData(val data: List<Bpm>) : MeasureMessage()
}

data class GoalTargets(
    val stepGoalTarget: Int = 10000,
    val highImpactGoalMinutes: Int = 30
)

