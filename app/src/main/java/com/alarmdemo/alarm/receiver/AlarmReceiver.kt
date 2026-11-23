package com.alarmdemo.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.alarmdemo.alarm.service.AlarmService
import com.alarmdemo.alarm.util.Constants

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "onReceive action: $action")

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            this.action = action
            putExtras(intent)
        }

        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting AlarmService", e)
        }
    }
}
