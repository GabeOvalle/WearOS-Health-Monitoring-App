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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.health.services.client.data.DataTypeAvailability
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.measuredatacompose.theme.MeasureDataTheme

@Composable
fun MeasureDataScreen(
    hr: Double,
    dailyAverage: Double?,
    heartRateAvailability: DataTypeAvailability,
    stepsEnabled: Boolean,
    alertMessage: String? = null,
    onDismissAlert: () -> Unit = {},
    stepCount: Int,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        //The BPM number changes colors based on how high the BPM is
        HrLabel(
            hr = hr,
            availability = heartRateAvailability,
            color = if (heartRateAvailability != DataTypeAvailability.AVAILABLE) Color.White
            else if (hr < 100) Color.Green
            else if (hr < 140) Color.Yellow
            else Color.Red
        )
        Text(
            text = "Today's Average BPM: ${dailyAverage?.toInt() ?: "--"}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = if (stepsEnabled) "Step Count: $stepCount" else "Step Count: --",
            style = MaterialTheme.typography.bodyMedium
        )
    }

    if (alertMessage != null) {
        AlertDialog(
            visible = true,
            onDismissRequest = onDismissAlert,
            title = {
                Text(
                    text = "Safety Alert",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            edgeButton = {
                EdgeButton(onClick = onDismissAlert) {
                    Text("OK")
                }
            },
            text = {
                Text(
                    text = alertMessage,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        )
    }
}

@Preview(
    device = WearDevices.SMALL_ROUND,
    showBackground = false,
    showSystemUi = true,
)
@Composable
fun MeasureDataScreenPreview() {
    MeasureDataTheme {
        MeasureDataScreen(
            hr = 65.0,
            dailyAverage = 95.0,
            heartRateAvailability = DataTypeAvailability.AVAILABLE,
            stepsEnabled = true,
            alertMessage = null,
            stepCount = 1234
        )
    }
}
