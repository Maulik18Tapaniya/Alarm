package com.alarmdemo.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.alarmdemo.alarm.data.repository.AlarmRepository
import com.alarmdemo.alarm.scheduler.AlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    @Inject
    lateinit var alarmRepository: AlarmRepository

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Device booted with action: $action, rescheduling alarms...")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val enabledAlarms = alarmRepository.getAllEnabledAlarms()
                    Log.d(TAG, "Restoring ${enabledAlarms.size} enabled alarms")
                    for (alarm in enabledAlarms) {
                        alarmScheduler.scheduleAlarm(alarm)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to reschedule alarms after boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
