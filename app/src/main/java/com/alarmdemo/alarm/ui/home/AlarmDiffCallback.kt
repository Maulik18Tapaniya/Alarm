package com.alarmdemo.alarm.ui.home

import androidx.recyclerview.widget.DiffUtil
import com.alarmdemo.alarm.data.local.AlarmEntity

class AlarmDiffCallback : DiffUtil.ItemCallback<AlarmEntity>() {
    override fun areItemsTheSame(oldItem: AlarmEntity, newItem: AlarmEntity): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: AlarmEntity, newItem: AlarmEntity): Boolean {
        return oldItem == newItem
    }
}
