package com.example.measuredatacompose.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Dialog
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.measuredatacompose.data.Goal
import java.time.Duration


@Composable
fun KeyMetricsScreen(
    stepGoal: Goal<Int>,
    highImpactTimeGoal: Goal<Duration>,
    onUpdateStepGoal: (Int) -> Unit,
    onUpdateHighImpactGoalMinutes: (Int) -> Unit,
) {
    val stepGoalIcon = if (stepGoal.isAchieved) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank
    val highImpactTimeGoalIcon = if (highImpactTimeGoal.isAchieved) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank
    val highImpactCurrent = highImpactTimeGoal.currentValue?.formatClock() ?: "N/A"
    val highImpactTarget = highImpactTimeGoal.targetValue?.formatClock() ?: "N/A"

    var showStepDialog by remember { mutableStateOf(false) }
    var showHighImpactDialog by remember { mutableStateOf(false) }
    var stepInput by remember { mutableStateOf("") }
    var highImpactInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            modifier = Modifier.padding(bottom = 16.dp),
            text = "Key Metrics",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )
        Row(
            modifier = Modifier.padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Absolute.Left,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.padding(horizontal = 8.dp),
                imageVector = stepGoalIcon,
                contentDescription = "Step Goal",
            )
            Button(
                onClick = { showStepDialog = true },
            ) {
                Text(
                    text = "Steps:\n${stepGoal.currentValue ?: "N/A"} / ${stepGoal.targetValue ?: "N/A"}",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Left
                )
            }

        }
        Row(
            horizontalArrangement = Arrangement.Absolute.Left,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.padding(horizontal = 8.dp),
                imageVector = highImpactTimeGoalIcon,
                contentDescription = "High Impact Time Goal",
            )
            Button(
                onClick = { showHighImpactDialog = true },
            ) {
                Text(
                    text = "HI Time:\n$highImpactCurrent / $highImpactTarget",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Left
                )
            }
        }
    }

    Dialog(
        visible = showStepDialog,
        onDismissRequest = {
            showStepDialog = false
            stepInput = ""
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Steps Target: $stepInput",
                textAlign = TextAlign.Center
            )
            Numpad(
                onNumberClick = { if (stepInput.length < 5) stepInput += it.toString() },
                onDelete = { stepInput = stepInput.dropLast(1) },
                onEnter = {
                    if (stepInput.isNotEmpty()) {
                        onUpdateStepGoal(stepInput.toInt())
                    }
                    showStepDialog = false
                    stepInput = ""
                }
            )
        }
    }

    Dialog(
        visible = showHighImpactDialog,
        onDismissRequest = {
            showHighImpactDialog = false
            highImpactInput = ""
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "HI Goal (min): $highImpactInput",
                textAlign = TextAlign.Center
            )
            Numpad(
                onNumberClick = { if (highImpactInput.length < 3) highImpactInput += it.toString() },
                onDelete = { highImpactInput = highImpactInput.dropLast(1) },
                onEnter = {
                    if (highImpactInput.isNotEmpty()) {
                        onUpdateHighImpactGoalMinutes(highImpactInput.toInt())
                    }
                    showHighImpactDialog = false
                    highImpactInput = ""
                }
            )
        }
    }
}

@Preview(
    device = WearDevices.SMALL_ROUND,
    showBackground = false,
    showSystemUi = true,
)
@Composable
fun KeyMetricsScreenPreview() {
    KeyMetricsScreen(
        stepGoal = Goal(100, 100),
        highImpactTimeGoal = Goal(Duration.ofMinutes(30), Duration.ofMinutes(20)),
        onUpdateStepGoal = {},
        onUpdateHighImpactGoalMinutes = {}
    )
}
