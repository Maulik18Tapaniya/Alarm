package com.alarmdemo.alarm.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alarmdemo.alarm.R
import com.alarmdemo.alarm.data.local.AlarmEntity
import com.alarmdemo.alarm.databinding.ItemAlarmBinding
import com.alarmdemo.alarm.util.DateTimeUtils

class AlarmAdapter(
    private val onItemClick: (AlarmEntity) -> Unit,
    private val onToggleEnabled: (AlarmEntity, Boolean) -> Unit,
    private val onToggleFavorite: (AlarmEntity, Boolean) -> Unit,
    private val onDelete: (AlarmEntity) -> Unit
) : ListAdapter<AlarmEntity, AlarmAdapter.AlarmViewHolder>(AlarmDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val binding = ItemAlarmBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AlarmViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AlarmViewHolder(
        private val binding: ItemAlarmBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(alarm: AlarmEntity) {
            val context = itemView.context

            binding.tvAlarmTime.text = DateTimeUtils.formatTime(alarm.hour, alarm.minute)
            binding.tvAlarmAmPm.text = DateTimeUtils.getAmPm(alarm.hour)

            binding.tvAlarmLabel.text = if (alarm.title.isNotBlank()) {
                alarm.title
            } else {
                context.getString(R.string.app_name)
            }

            binding.tvAlarmDays.text = DateTimeUtils.formatRepeatDays(
                context,
                alarm.repeatDays,
                alarm.oneTimeYear,
                alarm.oneTimeMonth,
                alarm.oneTimeDay
            )

            // Favorite state
            if (alarm.isFavorite) {
                binding.btnFavorite.setImageResource(R.drawable.ic_star_filled)
            } else {
                binding.btnFavorite.setImageResource(R.drawable.ic_star_outline)
            }

            // Enabled / Inactive state visual feedback
            if (alarm.isEnabled) {
                val nextTrigger = DateTimeUtils.calculateNextTriggerTime(alarm)
                binding.tvAlarmCountdown.text = "Alarm in " + DateTimeUtils.getTimeRemainingText(nextTrigger)
                binding.tvAlarmCountdown.visibility = View.VISIBLE
                binding.layoutItemContent.alpha = 1.0f
            } else {
                binding.tvAlarmCountdown.visibility = View.GONE
                binding.layoutItemContent.alpha = 0.5f
            }

            // Avoid re-triggering listener on scroll/rebinding
            binding.switchAlarm.setOnCheckedChangeListener(null)
            binding.switchAlarm.isChecked = alarm.isEnabled
            binding.switchAlarm.setOnCheckedChangeListener { _, isChecked ->
                onToggleEnabled(alarm, isChecked)
            }

            binding.btnFavorite.setOnClickListener {
                onToggleFavorite(alarm, !alarm.isFavorite)
            }

            binding.btnDelete.setOnClickListener {
                onDelete(alarm)
            }

            binding.cardAlarm.setOnClickListener {
                onItemClick(alarm)
            }
        }
    }
}
