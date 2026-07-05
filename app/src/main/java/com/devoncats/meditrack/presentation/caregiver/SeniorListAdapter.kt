package com.devoncats.meditrack.presentation.caregiver

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.devoncats.meditrack.R
import com.devoncats.meditrack.domain.model.MedicationLogStatus
import com.google.android.material.chip.Chip

class SeniorListAdapter(
    private val onItemClick: (SeniorListItem) -> Unit,
    private val onDeleteClick: (SeniorListItem) -> Unit
) : ListAdapter<SeniorListItem, SeniorListAdapter.SeniorViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeniorViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_senior, parent, false)
        return SeniorViewHolder(view, onItemClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: SeniorViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SeniorViewHolder(
        itemView: View,
        private val onItemClick: (SeniorListItem) -> Unit,
        private val onDeleteClick: (SeniorListItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val nameTextView = itemView.findViewById<TextView>(R.id.seniorName)
        private val statusChip = itemView.findViewById<Chip>(R.id.seniorStatusChip)
        private val deleteButton = itemView.findViewById<ImageButton>(R.id.deleteSeniorButton)

        fun bind(item: SeniorListItem) {
            nameTextView.text = item.senior.name
            statusChip.setText(
                when (item.todayStatus) {
                    MedicationLogStatus.CONFIRMED -> R.string.med_status_confirmed
                    MedicationLogStatus.PENDING -> R.string.med_status_pending
                    MedicationLogStatus.MISSED -> R.string.med_status_missed
                    null -> R.string.senior_status_no_doses_today
                }
            )
            itemView.setOnClickListener { onItemClick(item) }
            deleteButton.setOnClickListener { onDeleteClick(item) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<SeniorListItem>() {
        override fun areItemsTheSame(oldItem: SeniorListItem, newItem: SeniorListItem) =
            oldItem.senior.id == newItem.senior.id

        override fun areContentsTheSame(oldItem: SeniorListItem, newItem: SeniorListItem) =
            oldItem == newItem
    }
}
