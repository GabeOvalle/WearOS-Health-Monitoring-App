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

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.VerticalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AppScaffold
import com.example.measuredatacompose.PERMISSIONS
import com.example.measuredatacompose.data.HealthServicesRepository
import com.example.measuredatacompose.data.HeartRateService
import com.example.measuredatacompose.data.Goal
import com.example.measuredatacompose.data.StepPassiveTracker
import java.time.Duration
import com.example.measuredatacompose.theme.MeasureDataTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MeasureDataApp(
    healthServicesRepository: HealthServicesRepository,
    stepPassiveTracker: StepPassiveTracker
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("health_prefs", Context.MODE_PRIVATE)
    var isFlipped by remember {mutableStateOf(prefs.getBoolean("is_flipped", false))}
    val rotation = if (isFlipped) 180f else 0f
    MeasureDataTheme {
        AppScaffold(
            modifier = Modifier.fillMaxSize(),
        ) {
            val viewModel: MeasureDataViewModel = viewModel(
                factory = MeasureDataViewModelFactory(
                    healthServicesRepository = healthServicesRepository,
                    stepsPassiveTracker = stepPassiveTracker
                ),
            )

            val bpmDataMessage by viewModel.bpmDataMessage.collectAsStateWithLifecycle()
            val hr by viewModel.hr
            val stepCount by stepPassiveTracker.stepCount.collectAsState()
            val heartRateAvailability by viewModel.heartRateAvailability
            val uiState by viewModel.uiState
            val isRunning by healthServicesRepository.isServiceRunning.collectAsState()
            val stepsEnabled by stepPassiveTracker.started.collectAsState()
            val alertMessage by viewModel.alertMessage
            val dailyAverage by viewModel.dailyAverage
            val highImpactState by viewModel.highImpactState.collectAsState()
            val hourlyUi by viewModel.hourlyUi.collectAsState()
            val stepGoalTarget = viewModel.stepGoalTarget.intValue
            val highImpactGoalMinutes = viewModel.highImpactGoalMinutes.intValue

            val permissionState = rememberMultiplePermissionsState(
                permissions = PERMISSIONS,
            )

            LaunchedEffect(permissionState.allPermissionsGranted) {
                if (permissionState.allPermissionsGranted) {
                    Log.i("VIBRATION_DEBUG", "App: Permissions granted, starting service if needed")
                    // Only start if not already running (optional, but good for consistency)
                    if (!healthServicesRepository.isServiceRunning.value) {
                        Log.i("VIBRATION_DEBUG", "App: Requesting foreground service start")
                        val intent = Intent(context, HeartRateService::class.java)
                        context.startForegroundService(intent)
                    }
                } else {
                    Log.i("VIBRATION_DEBUG", "App: Launching permission request")
                    permissionState.launchMultiplePermissionRequest()
                }
            }

            val pagerState = rememberPagerState(pageCount = { 2 })

            if (uiState == UiState.Supported) {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize().rotate(rotation)
                ) { page ->
                    when (page) {
                        0 -> HorizontalPager(
                            state = rememberPagerState(pageCount = { 4 }),
                            modifier = Modifier.fillMaxSize()
                        ) { page2 ->
                            when (page2) {
                                0 -> MeasureDataScreen(
                                    hr = hr,
                                    dailyAverage = dailyAverage,
                                    heartRateAvailability = heartRateAvailability,
                                    stepsEnabled = stepsEnabled,
                                    alertMessage = alertMessage,
                                    stepCount = stepCount,
                                    onDismissAlert = { viewModel.dismissAlert() }
                                )
                                1 -> HighImpactScreen(
                                    totalDailyHIDuration = highImpactState.totalDailyDuration,
                                    burstHIDuration = highImpactState.burstDuration,
                                    vibrationExposure = highImpactState.vibrationExposure
                                )
                                2 -> HourlyDataScreen(hourlyUi)
                                3 -> KeyMetricsScreen(
                                    stepGoal = Goal(
                                        targetValue = stepGoalTarget,
                                        currentValue = stepCount
                                    ),
                                    highImpactTimeGoal = Goal(
                                        targetValue = Duration.ofMinutes(highImpactGoalMinutes.toLong()),
                                        currentValue = highImpactState.totalDailyDuration
                                    ),
                                    onUpdateStepGoal = { newTarget ->
                                        viewModel.updateStepGoalTarget(newTarget)
                                    },
                                    onUpdateHighImpactGoalMinutes = { newMinutes ->
                                        viewModel.updateHighImpactGoalMinutes(newMinutes)
                                    }
                                )
                            }
                        }
                        1 -> VerticalPager(
                            state = rememberPagerState(pageCount = { 2 }),
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            when (page) {
                                0 -> HeartRateBoundsScreen(
                                    {viewModel.dismissAlert()}, healthServicesRepository)
                                1 -> PreferencesScreen(
                                    isRunning = isRunning,
                                    isFlipped = isFlipped,
                                    onFlipChange = {
                                        isFlipped = it
                                        prefs.edit().putBoolean("is_flipped", it).apply()
                                    }
                                )
                            }
                        }
                    }
                }

            } else if (uiState == UiState.NotSupported) {
                NotSupportedScreen()
            }
        }
    }
}
