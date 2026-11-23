package com.alarmdemo.alarm

import com.alarmdemo.alarm.data.local.AlarmEntity
import com.alarmdemo.alarm.util.DateTimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DateTimeUtilsTest {

    @Test
    fun testFormatTime12Hour() {
        assertEquals("12:00", DateTimeUtils.formatTime(0, 0, false))
        assertEquals("12:30", DateTimeUtils.formatTime(12, 30, false))
        assertEquals("07:05", DateTimeUtils.formatTime(7, 5, false))
        assertEquals("09:45", DateTimeUtils.formatTime(21, 45, false))
    }

    @Test
    fun testFormatTime24Hour() {
        assertEquals("00:00", DateTimeUtils.formatTime(0, 0, true))
        assertEquals("07:05", DateTimeUtils.formatTime(7, 5, true))
        assertEquals("21:45", DateTimeUtils.formatTime(21, 45, true))
    }

    @Test
    fun testGetAmPm() {
        assertEquals("AM", DateTimeUtils.getAmPm(0))
        assertEquals("AM", DateTimeUtils.getAmPm(11))
        assertEquals("PM", DateTimeUtils.getAmPm(12))
        assertEquals("PM", DateTimeUtils.getAmPm(23))
    }

    @Test
    fun testOneTimeAlarm_FutureToday() {
        val baseCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val fromTime = baseCal.timeInMillis

        // Alarm at 9:00 AM (future today)
        val alarm = AlarmEntity(hour = 9, minute = 0)
        val trigger = DateTimeUtils.calculateNextTriggerTime(alarm, fromTime)

        val triggerCal = Calendar.getInstance().apply { timeInMillis = trigger }
        assertEquals(9, triggerCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, triggerCal.get(Calendar.MINUTE))
        assertEquals(baseCal.get(Calendar.DAY_OF_YEAR), triggerCal.get(Calendar.DAY_OF_YEAR))
        assertTrue(trigger > fromTime)
    }

    @Test
    fun testOneTimeAlarm_PastToday_SchedulesTomorrow() {
        val baseCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val fromTime = baseCal.timeInMillis

        // Alarm at 10:00 AM (past for today -> should schedule for tomorrow)
        val alarm = AlarmEntity(hour = 10, minute = 0)
        val trigger = DateTimeUtils.calculateNextTriggerTime(alarm, fromTime)

        val triggerCal = Calendar.getInstance().apply { timeInMillis = trigger }
        assertEquals(10, triggerCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, triggerCal.get(Calendar.MINUTE))
        assertTrue(trigger > fromTime)

        val expectedCal = (baseCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        assertEquals(expectedCal.get(Calendar.DAY_OF_YEAR), triggerCal.get(Calendar.DAY_OF_YEAR))
    }

    @Test
    fun testRecurringAlarm_NextOccurrence() {
        val baseCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val fromTime = baseCal.timeInMillis

        // Alarm repeating on Wednesday and Friday at 7:00 AM
        val alarm = AlarmEntity(
            hour = 7,
            minute = 0,
            repeatDays = setOf(Calendar.WEDNESDAY, Calendar.FRIDAY)
        )
        val trigger = DateTimeUtils.calculateNextTriggerTime(alarm, fromTime)
        val triggerCal = Calendar.getInstance().apply { timeInMillis = trigger }

        // Next trigger should be Wednesday at 7:00 AM
        assertEquals(Calendar.WEDNESDAY, triggerCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(7, triggerCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, triggerCal.get(Calendar.MINUTE))
        assertTrue(trigger > fromTime)
    }

    @Test
    fun testIsDateInPast() {
        val pastCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        assertTrue(
            DateTimeUtils.isDateInPast(
                pastCal.get(Calendar.YEAR),
                pastCal.get(Calendar.MONTH),
                pastCal.get(Calendar.DAY_OF_MONTH),
                12,
                0
            )
        )

        val futureCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 2)
        }
        assertFalse(
            DateTimeUtils.isDateInPast(
                futureCal.get(Calendar.YEAR),
                futureCal.get(Calendar.MONTH),
                futureCal.get(Calendar.DAY_OF_MONTH),
                12,
                0
            )
        )
    }
}
