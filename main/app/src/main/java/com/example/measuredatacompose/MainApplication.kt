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
package com.example.measuredatacompose

import android.app.Application
import android.os.Build
import com.example.measuredatacompose.data.HealthServicesRepository
import com.example.measuredatacompose.data.StepPassiveTracker

const val TAG = "Measure Data Sample"

// Runtime permissions required for the app to function
val PERMISSIONS = mutableListOf(
    android.Manifest.permission.BODY_SENSORS,
    android.Manifest.permission.ACTIVITY_RECOGNITION
).apply {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(android.Manifest.permission.POST_NOTIFICATIONS)
    }
}.toList()

class MainApplication : Application() {

    val stepPassiveTracker by lazy { StepPassiveTracker(this) }
    val healthServicesRepository by lazy { HealthServicesRepository(this, stepPassiveTracker)}
}