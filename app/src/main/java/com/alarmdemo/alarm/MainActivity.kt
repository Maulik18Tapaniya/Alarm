package com.alarmdemo.alarm

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.alarmdemo.alarm.data.local.AlarmEntity
import com.alarmdemo.alarm.databinding.ActivityMainBinding
import com.alarmdemo.alarm.ui.addedit.AddEditAlarmActivity
import com.alarmdemo.alarm.ui.home.AlarmAdapter
import com.alarmdemo.alarm.ui.home.AlarmFilter
import com.alarmdemo.alarm.ui.home.HomeViewModel
import com.alarmdemo.alarm.util.Constants
import com.alarmdemo.alarm.util.PermissionUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var alarmAdapter: AlarmAdapter

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(
                    this,
                    R.string.notification_permission_desc,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainCoordinator) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            binding.fabAddAlarm.translationY = -systemBars.bottom.toFloat()
            insets
        }

        setupRecyclerView()
        setupListeners()
        observeAlarms()
        checkAndRequestPermissions()
    }

    private fun setupRecyclerView() {
        alarmAdapter = AlarmAdapter(
            onItemClick = { alarm ->
                val intent = Intent(this, AddEditAlarmActivity::class.java).apply {
                    putExtra(Constants.EXTRA_ALARM_ID, alarm.id)
                }
                startActivity(intent)
            },
            onToggleEnabled = { alarm, isEnabled ->
                viewModel.toggleAlarm(alarm, isEnabled)
            },
            onToggleFavorite = { alarm, isFavorite ->
                viewModel.toggleFavorite(alarm, isFavorite)
            },
            onDelete = { alarm ->
                showDeleteConfirmDialog(alarm)
            }
        )

        binding.rvAlarms.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = alarmAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupListeners() {
        binding.fabAddAlarm.setOnClickListener {
            val intent = Intent(this, AddEditAlarmActivity::class.java)
            startActivity(intent)
        }

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            when (checkedIds.first()) {
                R.id.chipFilterAll -> viewModel.setFilter(AlarmFilter.ALL)
                R.id.chipFilterFavorites -> viewModel.setFilter(AlarmFilter.FAVORITES)
            }
        }
    }

    private fun observeAlarms() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.alarms.collect { alarms ->
                    alarmAdapter.submitList(alarms)
                    if (alarms.isEmpty()) {
                        binding.layoutEmptyState.visibility = View.VISIBLE
                        binding.rvAlarms.visibility = View.GONE
                    } else {
                        binding.layoutEmptyState.visibility = View.GONE
                        binding.rvAlarms.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun showDeleteConfirmDialog(alarm: AlarmEntity) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_alarm_title)
            .setMessage(R.string.delete_alarm_confirm)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteAlarm(alarm)
                Toast.makeText(this, R.string.alarm_deleted_toast, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun checkAndRequestPermissions() {
        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionUtils.hasNotificationPermission(this)) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Exact alarm permission (Android 12+)
        if (!PermissionUtils.canScheduleExactAlarms(this)) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.exact_alarm_permission_title)
                .setMessage(R.string.exact_alarm_permission_desc)
                .setPositiveButton(R.string.open_settings) { _, _ ->
                    PermissionUtils.openExactAlarmSettings(this)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }
}
