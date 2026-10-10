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

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonColors
import androidx.wear.compose.material3.Dialog
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.CheckboxButton
import androidx.wear.compose.material3.Dialog
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.measuredatacompose.data.HealthServicesRepository
import com.example.measuredatacompose.data.HeartRateService
import com.example.measuredatacompose.presentation.MeasureDataApp
import com.example.measuredatacompose.theme.MeasureDataTheme

@Composable
fun HeartRateBoundsScreen(
    onDismissRequest: () -> Unit,
    healthServicesRepository: HealthServicesRepository,
) {
    var ageSet by remember { mutableStateOf(false) }
    var manualSet by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var tempLowerBound by remember { mutableStateOf(healthServicesRepository.alertLowerBound.toInt().toString()) }
    var tempUpperBound by remember { mutableStateOf(healthServicesRepository.alertUpperBound.toInt().toString()) }
    var stagedLowerBound by remember {mutableStateOf("")}

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "HR Zone Configuration\nL: $tempLowerBound U:$tempUpperBound",
            textAlign = TextAlign.Center,
        )
        //Row containing buttons to select age or manual setting of the bounds
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Button(onClick = { ageSet = true }) {
                Text(
                    "Age"
                )
            }
            Button(onClick = { manualSet = true }) {
                Text(
                    "Manual"
                )
            }
        }


    }
    //The popup for the age setting of the lower and upper alert bounds.
    Dialog(
        visible = ageSet,
        onDismissRequest = {
            ageSet = false
            input = ""
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Age: $input"
            )
            //Numpad simplified to close after only one input
            Numpad(
                //Ensures the input cannot start with 0, and is at max 2 digits
                onNumberClick = { if ((input.isEmpty() && it != 0) || input.length == 1) input += it.toString() },
                onDelete = { input = input.dropLast(1) },
                onEnter = {
                    if (input.isNotEmpty() && input.toInt() > 0) {
                        healthServicesRepository.setAlertBounds(input.toInt())
                        tempLowerBound = if(input.toInt() >= 13) "60" else "75"
                        tempUpperBound = (220 - input.toInt()).toString()
                        input = ""
                        ageSet = false
                    } else if (input.isNotEmpty()) {
                        input = ""
                    } else {
                        ageSet = false
                    }
                }
            )
        }
    }
    //The popup that allows for the manual setting of the lower and upper alert bounds
    Dialog(visible = manualSet, onDismissRequest =  {
        manualSet = false
        input = ""
        stagedLowerBound = ""
    }
    ) {

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //Displays the thus-far entered bounds and the current input from the user
            Text(
                text = "L: $stagedLowerBound, U: $tempUpperBound, Input: $input",
                fontSize = 2.em
            )
            //Numpad modified to allow for two inputs in succession
            Numpad(
                //Ensures the input cannot start with 0, is at max 3 digits, and if it is three digits, it has to start with 1 or 2
                onNumberClick = { if ((input.isEmpty() && it != 0) || (input.length == 2 && (input.startsWith("1")) || input.startsWith("2")) || input.length == 1) input += it.toString() },
                onDelete = { input = input.dropLast(1) },
                onEnter = {
                    if (!input.isEmpty() && stagedLowerBound.isEmpty() && input.toInt() >= 60 && input.toInt() <= 100) {
                        stagedLowerBound = input
                        input = ""
                    } else if (!input.isEmpty() && !stagedLowerBound.isEmpty() && input.toInt() > tempLowerBound.toInt() && input.toInt() <= 200){
                        tempUpperBound = input
                        tempLowerBound = stagedLowerBound
                        healthServicesRepository.setAlertBounds(
                            tempLowerBound.toDouble(),
                            tempUpperBound.toDouble()
                        )
                        input = ""
                        stagedLowerBound = ""
                        manualSet = false
                    } else if (tempLowerBound.isEmpty() && input.isEmpty()){
                        manualSet = false
                    } else {
                        input = ""
                    }
                }
            )
        }
    }
}

