package com.alarmdemo.alarm.util

import android.content.Context
import com.alarmdemo.alarm.R
import com.alarmdemo.alarm.data.local.AlarmEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DateTimeUtils {

    fun formatTime(hour: Int, minute: Int, is24Hour: Boolean = false): String {
        return if (is24Hour) {
            String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        } else {
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            String.format(Locale.getDefault(), "%02d:%02d", displayHour, minute)
        }
    }

    fun getAmPm(hour: Int): String {
        return if (hour >= 12) "PM" else "AM"
    }

    fun calculateNextTriggerTime(alarm: AlarmEntity, fromTime: Long = System.currentTimeMillis()): Long {
        val nowCal = Calendar.getInstance().apply {
            timeInMillis = fromTime
        }

        if (alarm.repeatDays.isNotEmpty()) {
            for (i in 0..7) {
                val candidateCal = (nowCal.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, i)
                    set(Calendar.HOUR_OF_DAY, alarm.hour)
                    set(Calendar.MINUTE, alarm.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val dayOfWeek = candidateCal.get(Calendar.DAY_OF_WEEK)
                if (alarm.repeatDays.contains(dayOfWeek)) {
                    if (candidateCal.timeInMillis > fromTime) {
                        return candidateCal.timeInMillis
                    }
                }
            }
        }

        // One-time alarm
        val targetCal = (nowCal.clone() as Calendar).apply {
            if (alarm.oneTimeYear != null && alarm.oneTimeMonth != null && alarm.oneTimeDay != null) {
                set(Calendar.YEAR, alarm.oneTimeYear)
                set(Calendar.MONTH, alarm.oneTimeMonth)
                set(Calendar.DAY_OF_MONTH, alarm.oneTimeDay)
            }
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If no explicit date was specified and time today already passed, schedule for tomorrow
        if (alarm.oneTimeYear == null && targetCal.timeInMillis <= fromTime) {
            targetCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return targetCal.timeInMillis
    }

    fun formatRepeatDays(
        context: Context,
        repeatDays: Set<Int>,
        oneTimeYear: Int? = null,
        oneTimeMonth: Int? = null,
        oneTimeDay: Int? = null
    ): String {
        if (repeatDays.isEmpty()) {
            if (oneTimeYear != null && oneTimeMonth != null && oneTimeDay != null) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, oneTimeYear)
                    set(Calendar.MONTH, oneTimeMonth)
                    set(Calendar.DAY_OF_MONTH, oneTimeDay)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val todayCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val tomorrowCal = (todayCal.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                }

                return when (cal.timeInMillis) {
                    todayCal.timeInMillis -> context.getString(R.string.today)
                    tomorrowCal.timeInMillis -> context.getString(R.string.tomorrow)
                    else -> SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(cal.time)
                }
            }
            return context.getString(R.string.once)
        }

        if (repeatDays.size == 7) {
            return context.getString(R.string.everyday)
        }

        val weekdays = setOf(
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY
        )
        if (repeatDays == weekdays) {
            return context.getString(R.string.weekdays)
        }

        val weekends = setOf(Calendar.SATURDAY, Calendar.SUNDAY)
        if (repeatDays == weekends) {
            return context.getString(R.string.weekends)
        }

        val dayNames = mapOf(
            Calendar.SUNDAY to "Sun",
            Calendar.MONDAY to "Mon",
            Calendar.TUESDAY to "Tue",
            Calendar.WEDNESDAY to "Wed",
            Calendar.THURSDAY to "Thu",
            Calendar.FRIDAY to "Fri",
            Calendar.SATURDAY to "Sat"
        )

        val orderedDays = listOf(
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY,
            Calendar.SUNDAY
        )

        return orderedDays
            .filter { repeatDays.contains(it) }
            .mapNotNull { dayNames[it] }
            .joinToString(", ")
    }

    fun getTimeRemainingText(triggerMillis: Long): String {
        val diff = triggerMillis - System.currentTimeMillis()
        if (diff <= 0) return "Due now"

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        val remHours = hours % 24
        val remMinutes = minutes % 60

        return buildString {
            if (days > 0) {
                append("${days}d ")
            }
            if (remHours > 0 || days > 0) {
                append("${remHours}h ")
            }
            append("${remMinutes}m")
        }.trim()
    }

    fun isDateInPast(year: Int, month: Int, day: Int, hour: Int, minute: Int): Boolean {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis <= System.currentTimeMillis()
    }
}
