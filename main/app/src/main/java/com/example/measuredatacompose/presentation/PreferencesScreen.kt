package com.example.measuredatacompose.presentation

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.CheckboxButton
import androidx.wear.compose.material3.Text
import com.example.measuredatacompose.data.HeartRateService

@Composable
fun PreferencesScreen (
    isRunning: Boolean,
    isFlipped: Boolean,
    onFlipChange: (Boolean) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        //Users can turn the background service on or off by checking this checkbox
        CheckboxButton(
            checked = isRunning,
            onCheckedChange = {
                val intent = Intent(context, HeartRateService::class.java).apply {
                    if (isRunning) {
                        action = HeartRateService.ACTION_STOP_SERVICE
                    } else {
                        action = Intent.ACTION_RUN
                    }
                }
                context.startService(intent)
            },
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text(
                text = "Background Data Collection"
            )
        }

        CheckboxButton(
            checked = isFlipped,
            onCheckedChange = { checked ->
                onFlipChange(checked)
            }
        ) {
            Text(
                text = "Flip Screen"
            )
        }
    }

}