package com.example.measuredatacompose.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.measuredatacompose.PERMISSIONS
import com.example.measuredatacompose.TAG
import kotlin.jvm.java

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed, checking permissions before starting HeartRateService")
            
            val allPermissionsGranted = PERMISSIONS.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }

            if (allPermissionsGranted) {
                val serviceIntent = Intent(context, HeartRateService::class.java)
                context.startForegroundService(serviceIntent)
            } else {
                Log.w(TAG, "Permissions not granted after boot, cannot start HeartRateService")
            }
        }
    }
}
