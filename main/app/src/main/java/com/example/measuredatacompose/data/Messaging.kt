package com.example.measuredatacompose.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import java.util.concurrent.TimeUnit

/**
 * Singleton utility for MQTT messaging over Wi-Fi.
 * Implements an inactivity timeout and responds to network changes to save battery.
 */
object Messaging {
    private const val TAG = "Messaging"
    
    @Volatile
    private var mqttClient: Mqtt5AsyncClient? = null
    private var terminationJob: Job? = null
    private var retryJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var connectivityManager: ConnectivityManager? = null
    
    @Volatile
    private var appContext: Context? = null
    private var lastBrokerAddress: String? = null
    
    @Volatile
    private var isConnecting = false

    // Unique identifier for the physical watch (match Sim: e.g., "5")
    private var watchId: String = "5"
    // The employee ID this watch is currently assigned to (match Sim: e.g., "27")
    private var employeeId: String = "27"

    // 10.0.2.2 is the special alias for the host machine's loopback in the Android Emulator
    private const val EMULATOR_HOST = "10.0.2.2"
    // This is my computers IP address (for now) on my home network: 192.168.1.88
    // this is the local IP for my machine on labnet 10.199.252.234
    // manager portal IP: 172.27.32.1
    private const val REAL_DEVICE_HOST = "192.168.1.88"
    private const val DEFAULT_PORT = 1883

    private val _incomingCommands = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val incomingCommands: SharedFlow<String> = _incomingCommands.asSharedFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) {
            Log.d(TAG, "Network lost, disconnecting MQTT client")
            disconnect()
        }
        
        override fun onAvailable(network: Network) {
            Log.d(TAG, "Network available, triggering connection")
            appContext?.let { connect(it) }
        }
    }

    private fun isEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")
                || Build.BOARD.contains("goldfish")
    }

    private fun getAutoDetectedBroker(): String {
        return if (isEmulator()) EMULATOR_HOST else REAL_DEVICE_HOST
    }

    fun setWatchId(id: String) {
        watchId = id
    }

    fun setEmployeeId(id: String) {
        employeeId = id
    }

    private fun isWifiAvailable(context: Context): Boolean {
        if (isEmulator()) return true
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        // On Wear OS, 'activeNetwork' often points to the Bluetooth proxy.
        // To detect if Wi-Fi is available (e.g. for ADB or local MQTT), we inspect all networks.
        return cm.allNetworks.any { network ->
            val capabilities = cm.getNetworkCapabilities(network)
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true
        }
    }

    fun connect(context: Context, brokerAddress: String? = null) {
        synchronized(this) {
            appContext = context.applicationContext
            val targetBroker = brokerAddress ?: lastBrokerAddress ?: getAutoDetectedBroker()
            lastBrokerAddress = targetBroker
        }

        // Initialize connectivity manager and callback first so we can respond to changes
        if (connectivityManager == null) {
            connectivityManager = appContext?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
        }

        synchronized(this) {
            if (!isWifiAvailable(appContext!!)) {
                Log.w(TAG, "Wi-Fi not available, skipping connection")
                return
            }

            if (mqttClient?.state?.isConnected == true || isConnecting) return
            
            isConnecting = true
            retryJob?.cancel()
            retryJob = null
        }

        val target = lastBrokerAddress ?: getAutoDetectedBroker()
        Log.d(TAG, "Connecting to $target")
        mqttClient = Mqtt5Client.builder()
            .identifier("watch-$watchId")
            .serverHost(target)
            .serverPort(DEFAULT_PORT)
            .buildAsync()

        mqttClient?.connect()?.whenComplete { _, throwable ->
            synchronized(this) {
                isConnecting = false
                if (throwable != null) {
                    Log.w(TAG, "Broker not available: ${throwable.message}")
                    scheduleRetry()
                } else {
                    Log.d(TAG, "Connected successfully to $target")
                    subscribeToCommands()
                    startTerminationTimer()
                    sendEmployeeAssociation()
                    sendStatus("online")
                }
            }
        }
    }

    private fun scheduleRetry() {
        synchronized(this) {
            retryJob?.cancel()
            retryJob = scope.launch {
                Log.d(TAG, "Reconnection attempt scheduled in 10 seconds")
                delay(10_000)
                appContext?.let { connect(it) }
            }
        }
    }

    fun sendEmployeeAssociation() {
        val payload = EmployeeAssociationPayload(
            employeeId = employeeId,
            watchId = watchId,
            timestamp = System.currentTimeMillis()
        )
        
        val envelope = MqttEnvelope(
            topic = "employee",
            from = watchId,
            to = "manager",
            payload = Json.encodeToJsonElement(payload)
        )

        Log.d(TAG, "Sending association: Employee $employeeId to Watch $watchId")
        publish("watch/$watchId/employee", Json.encodeToString(envelope))
    }

    private fun subscribeToCommands() {
        val topic = "watch/$watchId/commands"
        mqttClient?.subscribeWith()
            ?.topicFilter(topic)
            ?.callback { publish ->
                val payload = String(publish.payloadAsBytes)
                Log.d(TAG, "Received command: Resetting inactivity timer")
                startTerminationTimer()
                scope.launch { _incomingCommands.emit(payload) }
            }
            ?.send()
    }

    fun sendHealthData(heartRate: Double?, steps: Int? = null, readiness: Int? = null, timestamp: Long? = null) {
        val payload = HealthDataPayload(
            watchId = watchId,
            heartRate = heartRate,
            steps = steps,
            readiness = readiness,
            timestamp = timestamp ?: System.currentTimeMillis()
        )
        
        val envelope = MqttEnvelope(
            topic = "data",
            from = watchId,
            to = "manager",
            payload = Json.encodeToJsonElement(payload)
        )

        publish("watch/$watchId/data", Json.encodeToString(envelope))
    }

    fun sendStatus(status: String) {
        val envelope = MqttEnvelope(
            topic = "status",
            from = watchId,
            to = "manager",
            payload = Json.encodeToJsonElement(status)
        )
        publish("watch/$watchId/status", Json.encodeToString(envelope))
    }

    fun sendAlert(alertMessage: String) {
        val envelope = MqttEnvelope(
            topic = "alert",
            from = watchId,
            to = "manager",
            payload = Json.encodeToJsonElement(alertMessage)
        )
        publish("watch/$watchId/alert", Json.encodeToString(envelope))
    }

    private fun publish(topic: String, jsonPayload: String) {
        startTerminationTimer()

        val client = mqttClient
        if (client == null || client.state.isConnected.not()) {
            appContext?.let { connect(it) }
            if (!isConnecting && (mqttClient == null || mqttClient?.state?.isConnected == false)) {
                Log.w(TAG, "Publish deferred: Client not connected")
                return
            }
        }

        client?.let {
            if (it.state.isConnected) {
                it.publishWith()
                    .topic(topic)
                    .payload(jsonPayload.toByteArray())
                    .send()
                    .whenComplete { _, _ ->
                        startTerminationTimer()
                    }
            }
        }
    }

    private fun startTerminationTimer() {
        synchronized(this) {
            terminationJob?.cancel()
            terminationJob = scope.launch {
                delay(60_000)
                Log.d(TAG, "Inactivity timeout reached, disconnecting MQTT")
                disconnect()
            }
        }
    }

    fun disconnect() {
        synchronized(this) {
            terminationJob?.cancel()
            terminationJob = null
            retryJob?.cancel()
            retryJob = null
            isConnecting = false
            mqttClient?.disconnect()
            mqttClient = null
            Log.d(TAG, "MQTT Disconnected")
        }
    }
}
