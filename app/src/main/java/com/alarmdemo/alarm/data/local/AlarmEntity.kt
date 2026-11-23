package com.alarmdemo.alarm.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val title: String = "",
    val isEnabled: Boolean = true,
    val isFavorite: Boolean = false,
    val repeatDays: Set<Int> = emptySet(),
    val oneTimeYear: Int? = null,
    val oneTimeMonth: Int? = null,
    val oneTimeDay: Int? = null,
    val snoozeDurationMinutes: Int = 10,
    val isVibrationEnabled: Boolean = true,
    val soundUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
