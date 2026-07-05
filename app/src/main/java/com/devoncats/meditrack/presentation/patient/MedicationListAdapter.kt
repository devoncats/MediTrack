package com.devoncats.meditrack.presentation.patient

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.devoncats.meditrack.R
import com.devoncats.meditrack.domain.model.MedicationLogStatus
import com.google.android.material.chip.Chip

class MedicationListAdapter(
    private val onItemClick: (MedicationListItem) -> Unit,
    private val onPendingStatusClick: (MedicationListItem) -> Unit
) : ListAdapter<MedicationListItem, MedicationListAdapter.MedicationViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_medication, parent, false)
        return MedicationViewHolder(view, onItemClick, onPendingStatusClick)
    }

    override fun onBindViewHolder(holder: MedicationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MedicationViewHolder(
        itemView: View,
        private val onItemClick: (MedicationListItem) -> Unit,
        private val onPendingStatusClick: (MedicationListItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val nameTextView = itemView.findViewById<TextView>(R.id.medicationName)
        private val doseTextView = itemView.findViewById<TextView>(R.id.medicationDose)
        private val statusChip = itemView.findViewById<Chip>(R.id.statusChip)

        fun bind(item: MedicationListItem) {
            nameTextView.text = item.medication.name
            doseTextView.text = item.medication.dose
            itemView.setOnClickListener { onItemClick(item) }

            statusChip.setText(
                when (item.todayStatus) {
                    MedicationLogStatus.CONFIRMED -> R.string.med_status_confirmed
                    MedicationLogStatus.MISSED -> R.string.med_status_missed
                    MedicationLogStatus.PENDING, null -> R.string.med_status_pending
                }
            )
            statusChip.setOnClickListener {
                if (item.todayStatus == MedicationLogStatus.PENDING) onPendingStatusClick(item)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<MedicationListItem>() {
        override fun areItemsTheSame(oldItem: MedicationListItem, newItem: MedicationListItem) =
            oldItem.medication.id == newItem.medication.id

        override fun areContentsTheSame(oldItem: MedicationListItem, newItem: MedicationListItem) =
            oldItem == newItem
    }
}
