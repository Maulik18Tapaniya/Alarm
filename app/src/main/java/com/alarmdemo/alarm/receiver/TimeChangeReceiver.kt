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
class TimeChangeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "TimeChangeReceiver"
    }

    @Inject
    lateinit var alarmRepository: AlarmRepository

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Time/timezone changed with action: $action, rescheduling alarms...")

        if (action == Intent.ACTION_TIME_CHANGED ||
            action == "android.intent.action.TIME_SET" ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_DATE_CHANGED
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val enabledAlarms = alarmRepository.getAllEnabledAlarms()
                    Log.d(TAG, "Re-aligning ${enabledAlarms.size} alarms following time change")
                    for (alarm in enabledAlarms) {
                        alarmScheduler.scheduleAlarm(alarm)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to reschedule alarms on time change", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
