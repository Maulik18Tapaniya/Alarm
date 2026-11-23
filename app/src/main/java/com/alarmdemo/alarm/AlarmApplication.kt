package com.alarmdemo.alarm

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.content.getSystemService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AlarmApplication : Application() {

    companion object {
        const val ALARM_NOTIFICATION_CHANNEL_ID = "alarm_clock_channel"
        const val SNOOZE_NOTIFICATION_CHANNEL_ID = "alarm_snooze_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService<NotificationManager>() ?: return

            // High priority channel for active ringing alarm
            val alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val alarmChannel = NotificationChannel(
                ALARM_NOTIFICATION_CHANNEL_ID,
                "Alarm Ringing Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for ringing alarm notifications and full-screen alerts"
                setSound(alarmSoundUri, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 500, 500)
                lockscreenVisibility = NotificationManager.IMPORTANCE_HIGH
            }

            // Low priority channel for snoozed alarm notification
            val snoozeChannel = NotificationChannel(
                SNOOZE_NOTIFICATION_CHANNEL_ID,
                "Snoozed Alarms",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for snoozed alarm status notifications"
                enableVibration(false)
            }

            notificationManager.createNotificationChannel(alarmChannel)
            notificationManager.createNotificationChannel(snoozeChannel)
        }
    }
}
