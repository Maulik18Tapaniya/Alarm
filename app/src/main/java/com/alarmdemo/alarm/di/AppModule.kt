package com.alarmdemo.alarm.di

import com.alarmdemo.alarm.data.repository.AlarmRepository
import com.alarmdemo.alarm.data.repository.AlarmRepositoryImpl
import com.alarmdemo.alarm.scheduler.AlarmScheduler
import com.alarmdemo.alarm.scheduler.AlarmSchedulerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindAlarmRepository(
        alarmRepositoryImpl: AlarmRepositoryImpl
    ): AlarmRepository

    @Binds
    @Singleton
    abstract fun bindAlarmScheduler(
        alarmSchedulerImpl: AlarmSchedulerImpl
    ): AlarmScheduler
}
