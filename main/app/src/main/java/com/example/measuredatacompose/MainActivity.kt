/*
 * File Name: MainActivity.kt
 * Author: Jamie Martig
 * Last Modified: 03-25-2026
 */

package com.example.measuredatacompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.measuredatacompose.data.Messaging
import com.example.measuredatacompose.presentation.MeasureDataApp

/**
 * Sets up the content view & initializes the navigation system for the app.
 *
 * @since 03-27-2026
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val healthServicesRepository = (application as MainApplication).healthServicesRepository
        val stepPassiveTracker = (application as MainApplication).stepPassiveTracker

        // Initialize MQTT connection to the manager portal
        Messaging.connect(this)

        setContent {
            MeasureDataApp(
                healthServicesRepository = healthServicesRepository,
                stepPassiveTracker = stepPassiveTracker
            )
        }
    }
}
