package com.alarmdemo.alarm.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alarmdemo.alarm.data.local.AlarmEntity
import com.alarmdemo.alarm.data.repository.AlarmRepository
import com.alarmdemo.alarm.scheduler.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AlarmFilter {
    ALL,
    FAVORITES
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val _filter = MutableStateFlow(AlarmFilter.ALL)
    val filter: StateFlow<AlarmFilter> = _filter

    @OptIn(ExperimentalCoroutinesApi::class)
    val alarms: StateFlow<List<AlarmEntity>> = _filter
        .flatMapLatest { filter ->
            when (filter) {
                AlarmFilter.ALL -> alarmRepository.getAllAlarms()
                AlarmFilter.FAVORITES -> alarmRepository.getFavoriteAlarms()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setFilter(filter: AlarmFilter) {
        _filter.value = filter
    }

    fun toggleAlarm(alarm: AlarmEntity, isEnabled: Boolean) {
        viewModelScope.launch {
            alarmRepository.setAlarmEnabled(alarm.id, isEnabled)
            val updated = alarm.copy(isEnabled = isEnabled)
            if (isEnabled) {
                alarmScheduler.scheduleAlarm(updated)
            } else {
                alarmScheduler.cancelAlarm(updated)
            }
        }
    }

    fun toggleFavorite(alarm: AlarmEntity, isFavorite: Boolean) {
        viewModelScope.launch {
            alarmRepository.setAlarmFavorite(alarm.id, isFavorite)
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            alarmScheduler.cancelAlarm(alarm)
            alarmRepository.deleteAlarm(alarm)
        }
    }
}
