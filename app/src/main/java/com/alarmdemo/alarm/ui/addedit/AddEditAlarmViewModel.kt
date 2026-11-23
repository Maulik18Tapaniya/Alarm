package com.alarmdemo.alarm.ui.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alarmdemo.alarm.data.local.AlarmEntity
import com.alarmdemo.alarm.data.repository.AlarmRepository
import com.alarmdemo.alarm.scheduler.AlarmScheduler
import com.alarmdemo.alarm.util.DateTimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class AddEditAlarmUiState(
    val alarmId: Long = 0,
    val isEditMode: Boolean = false,
    val hour: Int = 7,
    val minute: Int = 0,
    val title: String = "",
    val repeatDays: Set<Int> = emptySet(),
    val oneTimeYear: Int? = null,
    val oneTimeMonth: Int? = null,
    val oneTimeDay: Int? = null,
    val snoozeMinutes: Int = 10,
    val isVibrationEnabled: Boolean = true,
    val isEnabled: Boolean = true,
    val isFavorite: Boolean = false,
    val countdownText: String = "",
    val isLoading: Boolean = false
)

sealed interface AddEditUiEvent {
    data class SaveSuccess(val message: String) : AddEditUiEvent
    object DeleteSuccess : AddEditUiEvent
    data class ShowToast(val message: String) : AddEditUiEvent
}

@HiltViewModel
class AddEditAlarmViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditAlarmUiState())
    val uiState: StateFlow<AddEditAlarmUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<AddEditUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        // Default to upcoming 7:00 AM or current time + 1 hour
        val cal = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
        }
        _uiState.update {
            it.copy(
                hour = cal.get(Calendar.HOUR_OF_DAY),
                minute = cal.get(Calendar.MINUTE)
            )
        }
        recalculateCountdown()
    }

    fun loadAlarm(alarmId: Long) {
        if (alarmId <= 0) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val alarm = alarmRepository.getAlarmById(alarmId)
            if (alarm != null) {
                _uiState.update {
                    it.copy(
                        alarmId = alarm.id,
                        isEditMode = true,
                        hour = alarm.hour,
                        minute = alarm.minute,
                        title = alarm.title,
                        repeatDays = alarm.repeatDays,
                        oneTimeYear = alarm.oneTimeYear,
                        oneTimeMonth = alarm.oneTimeMonth,
                        oneTimeDay = alarm.oneTimeDay,
                        snoozeMinutes = alarm.snoozeDurationMinutes,
                        isVibrationEnabled = alarm.isVibrationEnabled,
                        isEnabled = alarm.isEnabled,
                        isFavorite = alarm.isFavorite,
                        isLoading = false
                    )
                }
                recalculateCountdown()
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun setTime(hour: Int, minute: Int) {
        _uiState.update {
            it.copy(hour = hour, minute = minute)
        }
        recalculateCountdown()
    }

    fun toggleRepeatDay(dayOfWeek: Int) {
        _uiState.update { current ->
            val updatedDays = if (current.repeatDays.contains(dayOfWeek)) {
                current.repeatDays - dayOfWeek
            } else {
                current.repeatDays + dayOfWeek
            }
            // If turning on recurring days, clear custom one-time date
            val (year, month, day) = if (updatedDays.isNotEmpty()) {
                Triple(null, null, null)
            } else {
                Triple(current.oneTimeYear, current.oneTimeMonth, current.oneTimeDay)
            }
            current.copy(
                repeatDays = updatedDays,
                oneTimeYear = year,
                oneTimeMonth = month,
                oneTimeDay = day
            )
        }
        recalculateCountdown()
    }

    fun setOneTimeDate(year: Int, month: Int, dayOfMonth: Int) {
        _uiState.update {
            it.copy(
                repeatDays = emptySet(), // Custom date selected -> one-time alarm
                oneTimeYear = year,
                oneTimeMonth = month,
                oneTimeDay = dayOfMonth
            )
        }
        recalculateCountdown()
    }

    fun setTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun setSnoozeMinutes(minutes: Int) {
        _uiState.update { it.copy(snoozeMinutes = minutes) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isVibrationEnabled = enabled) }
    }

    fun setEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isEnabled = enabled) }
    }

    private fun recalculateCountdown() {
        val state = _uiState.value
        val entity = AlarmEntity(
            id = state.alarmId,
            hour = state.hour,
            minute = state.minute,
            title = state.title,
            isEnabled = state.isEnabled,
            repeatDays = state.repeatDays,
            oneTimeYear = state.oneTimeYear,
            oneTimeMonth = state.oneTimeMonth,
            oneTimeDay = state.oneTimeDay,
            snoozeDurationMinutes = state.snoozeMinutes,
            isVibrationEnabled = state.isVibrationEnabled
        )
        val triggerMillis = DateTimeUtils.calculateNextTriggerTime(entity)
        val text = "Alarm in " + DateTimeUtils.getTimeRemainingText(triggerMillis)
        _uiState.update { it.copy(countdownText = text) }
    }

    fun saveAlarm() {
        val state = _uiState.value

        // Validation for one-time alarm with specific date
        if (state.repeatDays.isEmpty() && state.oneTimeYear != null &&
            state.oneTimeMonth != null && state.oneTimeDay != null
        ) {
            if (DateTimeUtils.isDateInPast(
                    state.oneTimeYear,
                    state.oneTimeMonth,
                    state.oneTimeDay,
                    state.hour,
                    state.minute
                )
            ) {
                viewModelScope.launch {
                    _uiEvent.send(AddEditUiEvent.ShowToast("Cannot set alarm in the past. Please select a future date/time."))
                }
                return
            }
        }

        viewModelScope.launch {
            val entity = AlarmEntity(
                id = state.alarmId,
                hour = state.hour,
                minute = state.minute,
                title = state.title.trim(),
                isEnabled = state.isEnabled,
                isFavorite = state.isFavorite,
                repeatDays = state.repeatDays,
                oneTimeYear = state.oneTimeYear,
                oneTimeMonth = state.oneTimeMonth,
                oneTimeDay = state.oneTimeDay,
                snoozeDurationMinutes = state.snoozeMinutes,
                isVibrationEnabled = state.isVibrationEnabled
            )

            if (state.isEditMode) {
                // Cancel previous schedule before updating
                alarmScheduler.cancelAlarmById(entity.id)
                alarmRepository.updateAlarm(entity)
                if (entity.isEnabled) {
                    alarmScheduler.scheduleAlarm(entity)
                }
            } else {
                val newId = alarmRepository.insertAlarm(entity)
                val insertedEntity = entity.copy(id = newId)
                if (insertedEntity.isEnabled) {
                    alarmScheduler.scheduleAlarm(insertedEntity)
                }
            }

            val nextTrigger = DateTimeUtils.calculateNextTriggerTime(entity)
            val remaining = DateTimeUtils.getTimeRemainingText(nextTrigger)
            _uiEvent.send(AddEditUiEvent.SaveSuccess("Alarm set for $remaining from now"))
        }
    }

    fun deleteAlarm() {
        val id = _uiState.value.alarmId
        if (id <= 0) return
        viewModelScope.launch {
            alarmScheduler.cancelAlarmById(id)
            alarmRepository.deleteAlarmById(id)
            _uiEvent.send(AddEditUiEvent.DeleteSuccess)
        }
    }
}
