package com.example.measuredatacompose.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Root MQTT message structure.
 * Using @SerialName ensures keys aren't renamed by R8/ProGuard.
 */
@Serializable
data class MqttEnvelope(
    @SerialName("topic") val topic: String,
    @SerialName("from") val from: String,
    @SerialName("to") val to: String,
    @SerialName("payload") val payload: JsonElement
)

/**
 * Health telemetry payload.
 */
@Serializable
data class HealthDataPayload(
    @SerialName("watchId") val watchId: String,
    @SerialName("heartRate") val heartRate: Double?,
    @SerialName("steps") val steps: Int?,
    @SerialName("readiness") val readiness: Int?,
    @SerialName("timestamp") val timestamp: Long
)

/**
 * Association payload.
 */
@Serializable
data class EmployeeAssociationPayload(
    @SerialName("employeeId") val employeeId: String,
    @SerialName("watchId") val watchId: String,
    @SerialName("timestamp") val timestamp: Long
)