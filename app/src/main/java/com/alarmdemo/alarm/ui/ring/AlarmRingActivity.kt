package com.alarmdemo.alarm.ui.ring

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.alarmdemo.alarm.R
import com.alarmdemo.alarm.databinding.ActivityAlarmRingBinding
import com.alarmdemo.alarm.service.AlarmService
import com.alarmdemo.alarm.util.Constants
import com.alarmdemo.alarm.util.DateTimeUtils

class AlarmRingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmRingBinding
    private var alarmId: Long = -1
    private var pulseAnimator: ObjectAnimator? = null

    private val dismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupLockscreenFlags()

        binding = ActivityAlarmRingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intentFilter = IntentFilter(Constants.ACTION_ALARM_DISMISS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(dismissReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(dismissReceiver, intentFilter)
        }

        extractDataAndSetupUI()
        setupListeners()
        startPulseAnimation()
    }

    private fun setupLockscreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KeyguardManager::class.java)
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun extractDataAndSetupUI() {
        alarmId = intent.getLongExtra(Constants.EXTRA_ALARM_ID, -1L)
        val title = intent.getStringExtra(Constants.EXTRA_ALARM_TITLE)
        val hour = intent.getIntExtra(Constants.EXTRA_ALARM_HOUR, 0)
        val minute = intent.getIntExtra(Constants.EXTRA_ALARM_MINUTE, 0)
        val snoozeMinutes = intent.getIntExtra(Constants.EXTRA_SNOOZE_MINUTES, 10)

        binding.tvRingTime.text = DateTimeUtils.formatTime(hour, minute)
        binding.tvRingAmPm.text = DateTimeUtils.getAmPm(hour)

        if (!title.isNullOrBlank()) {
            binding.tvRingTitle.text = title
            binding.tvRingTitle.visibility = View.VISIBLE
        } else {
            binding.tvRingTitle.text = getString(R.string.app_name)
        }

        binding.btnSnooze.text = getString(R.string.snooze) + " (${snoozeMinutes}m)"
    }

    private fun setupListeners() {
        binding.btnSnooze.setOnClickListener {
            val snoozeIntent = Intent(this, AlarmService::class.java).apply {
                action = Constants.ACTION_ALARM_SNOOZE
            }
            ContextCompat.startForegroundService(this, snoozeIntent)
            finish()
        }

        binding.btnStop.setOnClickListener {
            val dismissIntent = Intent(this, AlarmService::class.java).apply {
                action = Constants.ACTION_ALARM_DISMISS
            }
            ContextCompat.startForegroundService(this, dismissIntent)
            finish()
        }
    }

    private fun startPulseAnimation() {
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
            binding.ivAlarmIcon,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.15f, 1.0f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.15f, 1.0f),
            PropertyValuesHolder.ofFloat(View.ALPHA, 0.8f, 1.0f, 0.8f)
        ).apply {
            duration = 1200
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pulseAnimator?.cancel()
        try {
            unregisterReceiver(dismissReceiver)
        } catch (e: Exception) {
            // Ignore if already unregistered
        }
    }
}
