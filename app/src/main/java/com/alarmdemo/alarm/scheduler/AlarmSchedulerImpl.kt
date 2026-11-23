package com.alarmdemo.alarm.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.alarmdemo.alarm.MainActivity
import com.alarmdemo.alarm.data.local.AlarmEntity
import com.alarmdemo.alarm.receiver.AlarmReceiver
import com.alarmdemo.alarm.util.Constants
import com.alarmdemo.alarm.util.DateTimeUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AlarmScheduler {

    companion object {
        private const val TAG = "AlarmScheduler"
        private const val SNOOZE_CODE_OFFSET = 100000
    }

    private val alarmManager: AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    override fun scheduleAlarm(alarm: AlarmEntity) {
        if (!alarm.isEnabled) {
            cancelAlarm(alarm)
            return
        }

        val triggerTime = DateTimeUtils.calculateNextTriggerTime(alarm)
        if (triggerTime <= System.currentTimeMillis()) {
            Log.w(TAG, "Not scheduling alarm ${alarm.id} because trigger time is in the past: $triggerTime")
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = Constants.ACTION_ALARM_TRIGGER
            putExtra(Constants.EXTRA_ALARM_ID, alarm.id)
            putExtra(Constants.EXTRA_ALARM_TITLE, alarm.title)
            putExtra(Constants.EXTRA_ALARM_HOUR, alarm.hour)
            putExtra(Constants.EXTRA_ALARM_MINUTE, alarm.minute)
            putExtra(Constants.EXTRA_SNOOZE_MINUTES, alarm.snoozeDurationMinutes)
            putExtra(Constants.EXTRA_IS_VIBRATE, alarm.isVibrationEnabled)
            putExtra(Constants.EXTRA_IS_SNOOZE, false)
        }

        val requestCode = alarm.id.toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExact(triggerTime, showPendingIntent, pendingIntent)
        Log.d(TAG, "Scheduled alarm ${alarm.id} for $triggerTime")
    }

    override fun cancelAlarm(alarm: AlarmEntity) {
        cancelAlarmById(alarm.id)
    }

    override fun cancelAlarmById(alarmId: Long) {
        val requestCode = alarmId.toInt()
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = Constants.ACTION_ALARM_TRIGGER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager?.cancel(pendingIntent)
        pendingIntent.cancel()

        // Also cancel any snooze
        cancelSnooze(alarmId)
        Log.d(TAG, "Cancelled alarm $alarmId")
    }

    override fun scheduleSnooze(alarmId: Long, title: String, snoozeMinutes: Int, isVibrate: Boolean) {
        val snoozeTriggerTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        val requestCode = (alarmId + SNOOZE_CODE_OFFSET).toInt()

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = Constants.ACTION_ALARM_TRIGGER
            putExtra(Constants.EXTRA_ALARM_ID, alarmId)
            putExtra(Constants.EXTRA_ALARM_TITLE, title)
            putExtra(Constants.EXTRA_IS_SNOOZE, true)
            putExtra(Constants.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
            putExtra(Constants.EXTRA_IS_VIBRATE, isVibrate)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExact(snoozeTriggerTime, showPendingIntent, pendingIntent)
        Log.d(TAG, "Scheduled snooze for alarm $alarmId in $snoozeMinutes minutes")
    }

    override fun cancelSnooze(alarmId: Long) {
        val requestCode = (alarmId + SNOOZE_CODE_OFFSET).toInt()
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = Constants.ACTION_ALARM_TRIGGER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager?.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun scheduleExact(
        triggerTime: Long,
        showPendingIntent: PendingIntent,
        pendingIntent: PendingIntent
    ) {
        val manager = alarmManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (manager.canScheduleExactAlarms()) {
                    manager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent),
                        pendingIntent
                    )
                } else {
                    manager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                manager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent),
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while scheduling exact alarm, falling back to inexact", e)
            try {
                manager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } catch (ex: Exception) {
                Log.e(TAG, "Failed fallback alarm scheduling", ex)
            }
        }
    }
}
