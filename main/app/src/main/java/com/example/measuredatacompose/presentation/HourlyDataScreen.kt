package com.example.measuredatacompose.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.health.services.client.data.DataTypeAvailability
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.measuredatacompose.data.Bpm
import com.example.measuredatacompose.data.HourlyDataManager
import com.example.measuredatacompose.presentation.ExampleData.TEST_BPM_LIST
import com.example.measuredatacompose.theme.MeasureDataTheme
import java.time.Instant
import java.time.ZoneId

@Composable
fun HourlyDataScreen(message : String) {

    //Made this a ScalingLazyColumn to allow for scrolling
    val columnState = rememberScalingLazyListState()
    ScalingLazyColumn (
        modifier = Modifier
            .fillMaxSize(),
        state = columnState
    ) {
        item {
            Text(
                text = "Hourly BPM",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
        }
        item {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Left,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Preview(
    device = WearDevices.SMALL_ROUND,
    showBackground = true,
    showSystemUi = true,
)
@Composable
fun HourlyDataScreenPreview() {
    MeasureDataTheme {
        val hourlyDataManager = HourlyDataManager(TEST_BPM_LIST)
        HourlyDataScreen(hourlyDataManager.toString())
    }
}

object ExampleData {
    private fun getTimestamp(hour: Int): Instant? {
        val zone = ZoneId.systemDefault()
        return Instant.now()
            .atZone(zone)
            .toLocalDate()
            .atTime(hour, 0, 0)
            .atZone(zone)
            .toInstant()
    }

    private fun getBpm(value: Double, hour: Int): Bpm {
        return Bpm(
            value,
            getTimestamp(hour)!!,
            DataTypeAvailability.AVAILABLE,
            true,
            true
        )
    }

    val TEST_BPM_LIST: MutableList<Bpm> = listOf<Bpm?>(
        getBpm(1.0, 8),
        getBpm(2.0, 9),
        getBpm(3.0, 10),
        getBpm(4.0, 11),
        getBpm(5.0, 12)
    ) as MutableList<Bpm>
}
