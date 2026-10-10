package com.example.measuredatacompose.data

import androidx.health.services.client.data.DataTypeAvailability

data class Steps(
    val value: Long,
    val availability: DataTypeAvailability,
    val isAccurate: Boolean,
    val canRecord: Boolean
)