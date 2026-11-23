package com.alarmdemo.alarm.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.alarmdemo.alarm.AlarmApplication
import com.alarmdemo.alarm.MainActivity
import com.alarmdemo.alarm.R
import com.alarmdemo.alarm.data.repository.AlarmRepository
import com.alarmdemo.alarm.scheduler.AlarmScheduler
import com.alarmdemo.alarm.ui.ring.AlarmRingActivity
import com.alarmdemo.alarm.util.Constants
import com.alarmdemo.alarm.util.DateTimeUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmService : Service() {

    companion object {
        private const val TAG = "AlarmService"
        private const val AUTO_DISMISS_DELAY_MS = 10 * 60 * 1000L // 10 minutes auto-timeout
    }

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    lateinit var alarmRepository: AlarmRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())

    private var currentAlarmId: Long = -1
    private var currentAlarmTitle: String = ""
    private var currentSnoozeMinutes: Int = 10
    private var isVibrationEnabled: Boolean = true

    private val autoDismissRunnable = Runnable {
        Log.d(TAG, "Alarm auto-dismiss timeout reached after 10 minutes")
        handleDismiss()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent.action) {
            Constants.ACTION_ALARM_SNOOZE -> {
                handleSnooze()
                return START_NOT_STICKY
            }
            Constants.ACTION_ALARM_DISMISS -> {
                handleDismiss()
                return START_NOT_STICKY
            }
            else -> {
                val alarmId = intent.getLongExtra(Constants.EXTRA_ALARM_ID, -1L)
                val title = intent.getStringExtra(Constants.EXTRA_ALARM_TITLE) ?: ""
                val hour = intent.getIntExtra(Constants.EXTRA_ALARM_HOUR, 0)
                val minute = intent.getIntExtra(Constants.EXTRA_ALARM_MINUTE, 0)
                val isSnooze = intent.getBooleanExtra(Constants.EXTRA_IS_SNOOZE, false)
                val snoozeMinutes = intent.getIntExtra(Constants.EXTRA_SNOOZE_MINUTES, 10)
                val isVibrate = intent.getBooleanExtra(Constants.EXTRA_IS_VIBRATE, true)

                currentAlarmId = alarmId
                currentAlarmTitle = title
                currentSnoozeMinutes = snoozeMinutes
                isVibrationEnabled = isVibrate

                val notification = buildForegroundNotification(
                    alarmId = alarmId,
                    title = title,
                    hour = hour,
                    minute = minute,
                    isSnooze = isSnooze,
                    snoozeMinutes = snoozeMinutes,
                    isVibrate = isVibrate
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        Constants.ALARM_NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(Constants.ALARM_NOTIFICATION_ID, notification)
                }

                startAudio()
                if (isVibrationEnabled) {
                    startVibration()
                }

                // Launch Full Screen Activity directly as well for best UX
                launchRingActivity(alarmId, title, hour, minute, isSnooze, snoozeMinutes, isVibrate)

                // Schedule auto-dismiss after 10 minutes
                handler.removeCallbacks(autoDismissRunnable)
                handler.postDelayed(autoDismissRunnable, AUTO_DISMISS_DELAY_MS)
            }
        }

        return START_STICKY
    }

    private fun launchRingActivity(
        alarmId: Long,
        title: String,
        hour: Int,
        minute: Int,
        isSnooze: Boolean,
        snoozeMinutes: Int,
        isVibrate: Boolean
    ) {
        try {
            val ringIntent = Intent(this, AlarmRingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                putExtra(Constants.EXTRA_ALARM_ID, alarmId)
                putExtra(Constants.EXTRA_ALARM_TITLE, title)
                putExtra(Constants.EXTRA_ALARM_HOUR, hour)
                putExtra(Constants.EXTRA_ALARM_MINUTE, minute)
                putExtra(Constants.EXTRA_IS_SNOOZE, isSnooze)
                putExtra(Constants.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
                putExtra(Constants.EXTRA_IS_VIBRATE, isVibrate)
            }
            startActivity(ringIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Could not start AlarmRingActivity directly", e)
        }
    }

    private fun buildForegroundNotification(
        alarmId: Long,
        title: String,
        hour: Int,
        minute: Int,
        isSnooze: Boolean,
        snoozeMinutes: Int,
        isVibrate: Boolean
    ): Notification {
        val timeString = DateTimeUtils.formatTime(hour, minute)
        val displayTitle = if (title.isNotBlank()) title else getString(R.string.alarm_ringing)
        val contentText = if (isSnooze) {
            getString(R.string.alarm_snoozed_desc, snoozeMinutes)
        } else {
            timeString
        }

        // Full Screen Intent
        val fullScreenIntent = Intent(this, AlarmRingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(Constants.EXTRA_ALARM_ID, alarmId)
            putExtra(Constants.EXTRA_ALARM_TITLE, title)
            putExtra(Constants.EXTRA_ALARM_HOUR, hour)
            putExtra(Constants.EXTRA_ALARM_MINUTE, minute)
            putExtra(Constants.EXTRA_IS_SNOOZE, isSnooze)
            putExtra(Constants.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
            putExtra(Constants.EXTRA_IS_VIBRATE, isVibrate)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            alarmId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze
        val snoozeIntent = Intent(this, AlarmService::class.java).apply {
            action = Constants.ACTION_ALARM_SNOOZE
        }
        val snoozePendingIntent = PendingIntent.getService(
            this,
            1,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Dismiss
        val dismissIntent = Intent(this, AlarmService::class.java).apply {
            action = Constants.ACTION_ALARM_DISMISS
        }
        val dismissPendingIntent = PendingIntent.getService(
            this,
            2,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, AlarmApplication.ALARM_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(displayTitle)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(R.drawable.ic_snooze, getString(R.string.snooze), snoozePendingIntent)
            .addAction(R.drawable.ic_stop, getString(R.string.stop), dismissPendingIntent)
            .build()
    }

    private fun handleSnooze() {
        Log.d(TAG, "Handling snooze for alarm $currentAlarmId")
        alarmScheduler.scheduleSnooze(
            alarmId = currentAlarmId,
            title = currentAlarmTitle,
            snoozeMinutes = currentSnoozeMinutes,
            isVibrate = isVibrationEnabled
        )
        // Broadcast to close ring activity if open
        sendBroadcast(Intent(Constants.ACTION_ALARM_DISMISS).setPackage(packageName))
        stopServiceInternal()
    }

    private fun handleDismiss() {
        Log.d(TAG, "Handling dismiss for alarm $currentAlarmId")
        val alarmId = currentAlarmId
        serviceScope.launch {
            if (alarmId > 0) {
                val alarm = alarmRepository.getAlarmById(alarmId)
                if (alarm != null) {
                    if (alarm.repeatDays.isNotEmpty()) {
                        // Recurring: re-schedule for next day
                        alarmScheduler.scheduleAlarm(alarm)
                    } else {
                        // One-time: disable in DB
                        alarmRepository.setAlarmEnabled(alarmId, false)
                    }
                }
            }
        }
        // Broadcast to close ring activity if open
        sendBroadcast(Intent(Constants.ACTION_ALARM_DISMISS).setPackage(packageName))
        stopServiceInternal()
    }

    private fun startAudio() {
        try {
            var alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (alertUri == null) {
                alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            if (alertUri == null) {
                alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmService, alertUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start media player", e)
        }
    }

    private fun startVibration() {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            val pattern = longArrayOf(0, 800, 500, 800, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start vibration", e)
        }
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "AlarmClock::AlarmWakeLock"
            )?.apply {
                acquire(AUTO_DISMISS_DELAY_MS)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock", e)
        }
    }

    private fun releaseResources() {
        handler.removeCallbacks(autoDismissRunnable)
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing media player", e)
        }

        try {
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling vibration", e)
        }

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing wake lock", e)
        }
    }

    private fun stopServiceInternal() {
        releaseResources()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseResources()
        serviceScope.cancel()
    }
}
