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
package com.example.measuredatacompose.presentation

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.health.services.client.data.DataTypeAvailability
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.measuredatacompose.data.HealthServicesRepository
import com.example.measuredatacompose.data.HighImpactState
import com.example.measuredatacompose.data.MeasureMessage
import com.example.measuredatacompose.data.StepPassiveTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MeasureDataViewModel(
    private val healthServicesRepository: HealthServicesRepository,
    private val stepPassiveTracker: StepPassiveTracker
) : ViewModel() {
    val enabled: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val hr = mutableDoubleStateOf(0.0)
    val heartRateAvailability: MutableState<DataTypeAvailability> =
        mutableStateOf(DataTypeAvailability.UNKNOWN)

    val uiState: MutableState<UiState> = mutableStateOf(UiState.Startup)

    val alertMessage: MutableState<String?> = mutableStateOf(null)

    val highImpactState: StateFlow<HighImpactState> = healthServicesRepository.highImpactState
    val hourlyUi: StateFlow<String> = healthServicesRepository.hourlyUi
    val dailyAverage = mutableStateOf<Double?>(null)

    val stepGoalTarget = mutableIntStateOf(10000)
    val highImpactGoalMinutes = mutableIntStateOf(30)

    //Fixed the bpmData collection flow so that the UI can keep up with the background service
    val bpmDataMessage: StateFlow<MeasureMessage?> = healthServicesRepository.measureFlow()
        .onEach { message ->
            when (message) {
                is MeasureMessage.MeasureData -> {
                    message.data.lastOrNull()?.let { bpm ->
                        hr.doubleValue = bpm.value ?: 0.0
                        heartRateAvailability.value = bpm.availability
                        dailyAverage.value = healthServicesRepository.bpmDataManager.dailyAverage
                    }
                }
                is MeasureMessage.MeasureAvailability -> {
                    heartRateAvailability.value = message.availability
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(0),
            initialValue = null
        )

    var availableTime = 0

    init {
        viewModelScope.launch {
            val targets = withContext(Dispatchers.IO) {
                healthServicesRepository.loadGoalTargets()
            }
            stepGoalTarget.intValue = targets.stepGoalTarget
            highImpactGoalMinutes.intValue = targets.highImpactGoalMinutes
        }

        viewModelScope.launch {
            val supported = healthServicesRepository.hasHeartRateCapability()
            uiState.value = if (supported) {
                UiState.Supported
            } else {
                UiState.NotSupported
            }
        }

        viewModelScope.launch {
            healthServicesRepository.alerts.collect { message ->
                alertMessage.value = message
            }
        }
    }

    fun dismissAlert() {
        alertMessage.value = null
        healthServicesRepository.stopAlertSound()
    }

    fun updateStepGoalTarget(newTarget: Int) {
        stepGoalTarget.intValue = newTarget
        persistGoalTargets()
    }

    fun updateHighImpactGoalMinutes(newMinutes: Int) {
        highImpactGoalMinutes.intValue = newMinutes
        persistGoalTargets()
    }

    private fun persistGoalTargets() {
        val targets = com.example.measuredatacompose.data.GoalTargets(
            stepGoalTarget = stepGoalTarget.intValue,
            highImpactGoalMinutes = highImpactGoalMinutes.intValue
        )
        viewModelScope.launch(Dispatchers.IO) {
            healthServicesRepository.saveGoalTargets(targets)
        }
    }
}

class MeasureDataViewModelFactory(
    private val healthServicesRepository: HealthServicesRepository,
    private val stepsPassiveTracker: StepPassiveTracker
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MeasureDataViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MeasureDataViewModel(
                healthServicesRepository = healthServicesRepository,
                stepPassiveTracker = stepsPassiveTracker
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

sealed class UiState {
    data object Startup : UiState()
    data object NotSupported : UiState()
    data object Supported : UiState()
}

