package com.alarmdemo.alarm.util

object Constants {
    const val ACTION_ALARM_TRIGGER = "com.alarmdemo.alarm.ACTION_ALARM_TRIGGER"
    const val ACTION_ALARM_SNOOZE = "com.alarmdemo.alarm.ACTION_ALARM_SNOOZE"
    const val ACTION_ALARM_DISMISS = "com.alarmdemo.alarm.ACTION_ALARM_DISMISS"
    const val ACTION_SHOW_RING = "com.alarmdemo.alarm.ACTION_SHOW_RING"

    const val EXTRA_ALARM_ID = "extra_alarm_id"
    const val EXTRA_ALARM_TITLE = "extra_alarm_title"
    const val EXTRA_ALARM_HOUR = "extra_alarm_hour"
    const val EXTRA_ALARM_MINUTE = "extra_alarm_minute"
    const val EXTRA_IS_SNOOZE = "extra_is_snooze"
    const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
    const val EXTRA_IS_VIBRATE = "extra_is_vibrate"

    const val ALARM_NOTIFICATION_ID = 1001
    const val SNOOZE_NOTIFICATION_ID = 1002
}
