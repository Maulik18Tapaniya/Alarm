package com.alarmdemo.alarm.scheduler

import com.alarmdemo.alarm.data.local.AlarmEntity

interface AlarmScheduler {
    fun scheduleAlarm(alarm: AlarmEntity)
    fun cancelAlarm(alarm: AlarmEntity)
    fun cancelAlarmById(alarmId: Long)
    fun scheduleSnooze(alarmId: Long, title: String, snoozeMinutes: Int, isVibrate: Boolean)
    fun cancelSnooze(alarmId: Long)
}
