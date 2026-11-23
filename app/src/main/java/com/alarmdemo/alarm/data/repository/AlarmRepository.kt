package com.alarmdemo.alarm.data.repository

import com.alarmdemo.alarm.data.local.AlarmEntity
import kotlinx.coroutines.flow.Flow

interface AlarmRepository {
    fun getAllAlarms(): Flow<List<AlarmEntity>>
    fun getFavoriteAlarms(): Flow<List<AlarmEntity>>
    suspend fun getAllEnabledAlarms(): List<AlarmEntity>
    suspend fun getAlarmById(id: Long): AlarmEntity?
    fun getAlarmByIdFlow(id: Long): Flow<AlarmEntity?>
    suspend fun insertAlarm(alarm: AlarmEntity): Long
    suspend fun updateAlarm(alarm: AlarmEntity)
    suspend fun deleteAlarm(alarm: AlarmEntity)
    suspend fun deleteAlarmById(id: Long)
    suspend fun setAlarmEnabled(id: Long, isEnabled: Boolean)
    suspend fun setAlarmFavorite(id: Long, isFavorite: Boolean)
}
