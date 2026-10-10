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
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.measuredatacompose.theme.MeasureDataTheme
import java.time.Duration

@Composable
fun HighImpactScreen(
    totalDailyHIDuration: Duration,
    burstHIDuration: Duration,
    vibrationExposure: Duration,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Vibration Exposure",
            style = MaterialTheme.typography.titleMedium,
            color = Color.Yellow,
            textAlign = TextAlign.Center
        )
        Text(
            modifier = Modifier.padding(bottom = 8.dp),
            text = vibrationExposure.formatClock(),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Last High Impact Burst",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )
        Text(
            modifier = Modifier.padding(bottom = 8.dp),
            text = burstHIDuration.formatClock(),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Total High Impact Activity",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )
        Text(
            text = totalDailyHIDuration.formatClock(),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(
    device = WearDevices.SMALL_ROUND,
    showBackground = true,
    showSystemUi = true,
)
@Composable
fun HighImpactScreenPreview() {
    MeasureDataTheme {
        HighImpactScreen(
            totalDailyHIDuration = Duration.ofSeconds(10),
            burstHIDuration = Duration.ofSeconds(5),
            vibrationExposure = Duration.ofMinutes(2)
        )
    }
}