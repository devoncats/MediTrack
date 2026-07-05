package com.devoncats.meditrack.presentation.caregiver

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.devoncats.meditrack.R
import com.devoncats.meditrack.domain.model.MissedDoseAlert
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MissedDoseAlertAdapter(
    private val onItemClick: (MissedDoseAlert) -> Unit
) : ListAdapter<MissedDoseAlert, MissedDoseAlertAdapter.MissedDoseAlertViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MissedDoseAlertViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_missed_dose_alert, parent, false)
        return MissedDoseAlertViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: MissedDoseAlertViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MissedDoseAlertViewHolder(
        itemView: View,
        private val onItemClick: (MissedDoseAlert) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val seniorNameTextView = itemView.findViewById<TextView>(R.id.missedDoseSeniorName)
        private val medicationNameTextView = itemView.findViewById<TextView>(R.id.missedDoseMedicationName)
        private val timeTextView = itemView.findViewById<TextView>(R.id.missedDoseTime)
        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        fun bind(item: MissedDoseAlert) {
            seniorNameTextView.text = item.seniorName
            medicationNameTextView.text = item.medicationName
            timeTextView.text = Instant.ofEpochMilli(item.scheduledDatetime)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
                .format(timeFormatter)
            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<MissedDoseAlert>() {
        override fun areItemsTheSame(oldItem: MissedDoseAlert, newItem: MissedDoseAlert) =
            oldItem.logId == newItem.logId

        override fun areContentsTheSame(oldItem: MissedDoseAlert, newItem: MissedDoseAlert) =
            oldItem == newItem
    }
}
