package com.alarmdemo.alarm.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromDaySet(days: Set<Int>?): String {
        if (days.isNullOrEmpty()) return ""
        return days.joinToString(",")
    }

    @TypeConverter
    fun toDaySet(value: String?): Set<Int> {
        if (value.isNullOrBlank()) return emptySet()
        return value.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
    }
}
