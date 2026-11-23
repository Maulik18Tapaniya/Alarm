package com.alarmdemo.alarm.data.repository

import com.alarmdemo.alarm.data.local.AlarmDao
import com.alarmdemo.alarm.data.local.AlarmEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmRepositoryImpl @Inject constructor(
    private val alarmDao: AlarmDao
) : AlarmRepository {

    override fun getAllAlarms(): Flow<List<AlarmEntity>> = alarmDao.getAllAlarms()

    override fun getFavoriteAlarms(): Flow<List<AlarmEntity>> = alarmDao.getFavoriteAlarms()

    override suspend fun getAllEnabledAlarms(): List<AlarmEntity> = alarmDao.getAllEnabledAlarms()

    override suspend fun getAlarmById(id: Long): AlarmEntity? = alarmDao.getAlarmById(id)

    override fun getAlarmByIdFlow(id: Long): Flow<AlarmEntity?> = alarmDao.getAlarmByIdFlow(id)

    override suspend fun insertAlarm(alarm: AlarmEntity): Long = alarmDao.insertAlarm(alarm)

    override suspend fun updateAlarm(alarm: AlarmEntity) {
        alarmDao.updateAlarm(alarm)
    }

    override suspend fun deleteAlarm(alarm: AlarmEntity) {
        alarmDao.deleteAlarm(alarm)
    }

    override suspend fun deleteAlarmById(id: Long) {
        alarmDao.deleteAlarmById(id)
    }

    override suspend fun setAlarmEnabled(id: Long, isEnabled: Boolean) {
        alarmDao.updateAlarmEnabled(id, isEnabled)
    }

    override suspend fun setAlarmFavorite(id: Long, isFavorite: Boolean) {
        alarmDao.updateAlarmFavorite(id, isFavorite)
    }
}
