package com.alarmdemo.alarm.ui.addedit

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.alarmdemo.alarm.R
import com.alarmdemo.alarm.databinding.ActivityAddEditAlarmBinding
import com.alarmdemo.alarm.util.Constants
import com.alarmdemo.alarm.util.DateTimeUtils
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@AndroidEntryPoint
class AddEditAlarmActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditAlarmBinding
    private val viewModel: AddEditAlarmViewModel by viewModels()

    private var isUpdatingUi = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.addEditCoordinator) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val alarmId = intent.getLongExtra(Constants.EXTRA_ALARM_ID, -1L)
        if (alarmId > 0) {
            viewModel.loadAlarm(alarmId)
        }

        setupToolbar()
        setupListeners()
        observeState()
        observeEvents()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.btnSave.setOnClickListener {
            viewModel.saveAlarm()
        }
    }

    private fun setupListeners() {
        binding.cardTimePicker.setOnClickListener {
            showTimePicker()
        }

        binding.layoutDatePicker.setOnClickListener {
            showDatePicker()
        }

        binding.btnPickDate.setOnClickListener {
            showDatePicker()
        }

        // Day of week buttons
        binding.btnDayMon.setOnClickListener { viewModel.toggleRepeatDay(Calendar.MONDAY) }
        binding.btnDayTue.setOnClickListener { viewModel.toggleRepeatDay(Calendar.TUESDAY) }
        binding.btnDayWed.setOnClickListener { viewModel.toggleRepeatDay(Calendar.WEDNESDAY) }
        binding.btnDayThu.setOnClickListener { viewModel.toggleRepeatDay(Calendar.THURSDAY) }
        binding.btnDayFri.setOnClickListener { viewModel.toggleRepeatDay(Calendar.FRIDAY) }
        binding.btnDaySat.setOnClickListener { viewModel.toggleRepeatDay(Calendar.SATURDAY) }
        binding.btnDaySun.setOnClickListener { viewModel.toggleRepeatDay(Calendar.SUNDAY) }

        // Title input
        binding.etTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!isUpdatingUi) {
                    viewModel.setTitle(s?.toString() ?: "")
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Snooze ChipGroup
        binding.chipGroupSnooze.setOnCheckedStateChangeListener { _, checkedIds ->
            if (isUpdatingUi || checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val minutes = when (checkedIds.first()) {
                R.id.chipSnooze5 -> 5
                R.id.chipSnooze10 -> 10
                R.id.chipSnooze15 -> 15
                R.id.chipSnooze30 -> 30
                else -> 10
            }
            viewModel.setSnoozeMinutes(minutes)
        }

        // Vibrate toggle
        binding.switchVibrate.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingUi) {
                viewModel.setVibrationEnabled(isChecked)
            }
        }

        // Enabled toggle
        binding.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingUi) {
                viewModel.setEnabled(isChecked)
            }
        }

        // Delete button
        binding.btnDelete.setOnClickListener {
            showDeleteConfirmDialog()
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    isUpdatingUi = true

                    // Title
                    binding.toolbar.title = if (state.isEditMode) {
                        getString(R.string.edit_alarm)
                    } else {
                        getString(R.string.new_alarm)
                    }
                    binding.btnDelete.visibility = if (state.isEditMode) View.VISIBLE else View.GONE

                    // Time display
                    binding.tvDisplayTime.text = DateTimeUtils.formatTime(state.hour, state.minute)
                    binding.tvDisplayAmPm.text = DateTimeUtils.getAmPm(state.hour)
                    binding.tvCountdownPreview.text = state.countdownText

                    // Day repeat chips
                    binding.btnDayMon.isSelected = state.repeatDays.contains(Calendar.MONDAY)
                    binding.btnDayTue.isSelected = state.repeatDays.contains(Calendar.TUESDAY)
                    binding.btnDayWed.isSelected = state.repeatDays.contains(Calendar.WEDNESDAY)
                    binding.btnDayThu.isSelected = state.repeatDays.contains(Calendar.THURSDAY)
                    binding.btnDayFri.isSelected = state.repeatDays.contains(Calendar.FRIDAY)
                    binding.btnDaySat.isSelected = state.repeatDays.contains(Calendar.SATURDAY)
                    binding.btnDaySun.isSelected = state.repeatDays.contains(Calendar.SUNDAY)

                    // Repeat summary
                    binding.tvRepeatSummary.text = DateTimeUtils.formatRepeatDays(
                        this@AddEditAlarmActivity,
                        state.repeatDays,
                        state.oneTimeYear,
                        state.oneTimeMonth,
                        state.oneTimeDay
                    )

                    // Date display
                    if (state.repeatDays.isNotEmpty()) {
                        binding.tvSelectedDate.text = getString(R.string.everyday)
                    } else if (state.oneTimeYear != null && state.oneTimeMonth != null && state.oneTimeDay != null) {
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, state.oneTimeYear)
                            set(Calendar.MONTH, state.oneTimeMonth)
                            set(Calendar.DAY_OF_MONTH, state.oneTimeDay)
                        }
                        binding.tvSelectedDate.text = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(cal.time)
                    } else {
                        binding.tvSelectedDate.text = getString(R.string.once)
                    }

                    // Title field (only update if text differs)
                    if (binding.etTitle.text?.toString() != state.title) {
                        binding.etTitle.setText(state.title)
                    }

                    // Snooze Chip
                    val targetChipId = when (state.snoozeMinutes) {
                        5 -> R.id.chipSnooze5
                        10 -> R.id.chipSnooze10
                        15 -> R.id.chipSnooze15
                        30 -> R.id.chipSnooze30
                        else -> R.id.chipSnooze10
                    }
                    if (binding.chipGroupSnooze.checkedChipId != targetChipId) {
                        binding.chipGroupSnooze.check(targetChipId)
                    }

                    // Switches
                    binding.switchVibrate.isChecked = state.isVibrationEnabled
                    binding.switchEnabled.isChecked = state.isEnabled

                    isUpdatingUi = false
                }
            }
        }
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEvent.collect { event ->
                    when (event) {
                        is AddEditUiEvent.SaveSuccess -> {
                            Toast.makeText(this@AddEditAlarmActivity, event.message, Toast.LENGTH_SHORT).show()
                            setResult(RESULT_OK)
                            finish()
                        }
                        is AddEditUiEvent.DeleteSuccess -> {
                            Toast.makeText(this@AddEditAlarmActivity, R.string.alarm_deleted_toast, Toast.LENGTH_SHORT).show()
                            setResult(RESULT_OK)
                            finish()
                        }
                        is AddEditUiEvent.ShowToast -> {
                            Toast.makeText(this@AddEditAlarmActivity, event.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun showTimePicker() {
        val currentState = viewModel.uiState.value
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(currentState.hour)
            .setMinute(currentState.minute)
            .setTitleText(getString(R.string.time))
            .build()

        picker.addOnPositiveButtonClickListener {
            viewModel.setTime(picker.hour, picker.minute)
        }

        picker.show(supportFragmentManager, "MaterialTimePicker")
    }

    private fun showDatePicker() {
        val constraintsBuilder = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointForward.now())

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.date))
            .setCalendarConstraints(constraintsBuilder.build())
            .build()

        picker.addOnPositiveButtonClickListener { selectedEpochMillis ->
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = selectedEpochMillis
            }
            viewModel.setOneTimeDate(
                year = cal.get(Calendar.YEAR),
                month = cal.get(Calendar.MONTH),
                dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            )
        }

        picker.show(supportFragmentManager, "MaterialDatePicker")
    }

    private fun showDeleteConfirmDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_alarm_title)
            .setMessage(R.string.delete_alarm_confirm)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteAlarm()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
