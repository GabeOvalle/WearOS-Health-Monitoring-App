package com.example.measuredatacompose.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.measuredatacompose.MainApplication
import com.example.measuredatacompose.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class HeartRateService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var repository: HealthServicesRepository

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): HeartRateService = this@HeartRateService
    }

    override fun onCreate() {
        super.onCreate()
        Log.i("VIBRATION_DEBUG", "Service: onCreate called")
        repository = (application as MainApplication).healthServicesRepository
        repository.setServiceRunning(true)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("VIBRATION_DEBUG", "Service: onStartCommand action: ${intent?.action}")
        if (intent?.action == ACTION_STOP_SERVICE) {
            Log.i("VIBRATION_DEBUG", "Service: Stopping service via action")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundService()
        observeData()
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = createNotification()

        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        } else {
            0
        }

        Log.i("VIBRATION_DEBUG", "Service: Starting foreground with type: $serviceType")

        try {
            if (serviceType != 0) {
                startForeground(NOTIFICATION_ID, notification, serviceType)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e("VIBRATION_DEBUG", "Service: Failed to start foreground", e)
            // If we fail to start foreground, we must stop the service to avoid ForegroundServiceDidNotStartInTimeException
            stopSelf()
        }
    }

    private fun observeData() {
        Log.i("VIBRATION_DEBUG", "Service: observeData() triggered")
        serviceScope.launch {
            Log.i("VIBRATION_DEBUG", "Service: HR collection started")
            repository.measureFlow().collect()
        }
        serviceScope.launch {
            Log.i("VIBRATION_DEBUG", "Service: Vibration collection started")
            repository.vibrationFlow.collect {
                // Raw trace for debugging
                //Log.v("VIBRATION_DEBUG", "Service: Mag: ${it.magnitude}")
            }
        }
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, HeartRateService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Health & Vibration Tracker")
            .setContentText("Monitoring in background...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .addAction(R.drawable.ic_launcher_foreground, "Stop", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Health Data Recording Channel",
                NotificationManager.IMPORTANCE_LOW // Lower importance to be less intrusive
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("VIBRATION_DEBUG", "Service: onDestroy called")
        repository.setServiceRunning(false)
        serviceScope.cancel()
    }

    companion object {
        private const val CHANNEL_ID = "HeartRateServiceChannel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_STOP_SERVICE = "STOP_HEART_RATE_SERVICE"
    }
}

