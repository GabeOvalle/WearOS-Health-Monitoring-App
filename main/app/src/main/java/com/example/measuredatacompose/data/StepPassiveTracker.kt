package com.example.measuredatacompose.data

import android.content.Context
import android.util.Log
import androidx.health.services.client.HealthServices
import androidx.health.services.client.PassiveListenerCallback
import androidx.health.services.client.PassiveMonitoringClient
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.PassiveListenerConfig
import kotlinx.coroutines.flow.MutableStateFlow

class StepPassiveTracker(context: Context) {

    private val passiveClient: PassiveMonitoringClient =
        HealthServices.getClient(context).passiveMonitoringClient

    val stepCount = MutableStateFlow(0)

    var started = MutableStateFlow(false)

    val callback = object : PassiveListenerCallback {

        override fun onNewDataPointsReceived(dataPoints: DataPointContainer) {
            val stepsEvents = dataPoints.getData(DataType.STEPS_DAILY).forEach {
                stepCount.value = it.value.toInt()
            }

        }

        override fun onRegistrationFailed(throwable: Throwable) {
            started.value = false
            Log.e("Steps", "Passive tracking failed", throwable)
        }
    }

    fun start() {
        started.value = true
        val config = PassiveListenerConfig.builder()
            .setDataTypes(setOf(DataType.STEPS_DAILY))
            .build()

        passiveClient.setPassiveListenerCallback(config, callback)
    }

    fun stop() {
        started.value = false
        passiveClient.clearPassiveListenerCallbackAsync()
    }

}

sealed class StepsMessage {
    class StepsData(val data: List<Steps>) : StepsMessage()
}