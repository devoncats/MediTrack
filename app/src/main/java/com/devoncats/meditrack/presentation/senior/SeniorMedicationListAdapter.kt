package com.devoncats.meditrack.presentation.senior

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.devoncats.meditrack.R
import com.devoncats.meditrack.services.FileStorageHelper
import com.devoncats.meditrack.utils.StatusChipBinder
import com.google.android.material.chip.Chip

class SeniorMedicationListAdapter : ListAdapter<SeniorMedicationItem, SeniorMedicationListAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_senior_medication, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val thumbnailImageView = itemView.findViewById<ImageView>(R.id.seniorMedicationThumbnail)
        private val nameTextView = itemView.findViewById<TextView>(R.id.seniorMedicationName)
        private val doseTextView = itemView.findViewById<TextView>(R.id.seniorMedicationDose)
        private val scheduleTextView = itemView.findViewById<TextView>(R.id.seniorMedicationSchedule)
        private val statusChip = itemView.findViewById<Chip>(R.id.seniorMedicationStatusChip)
        private val fileStorageHelper = FileStorageHelper(itemView.context)

        fun bind(item: SeniorMedicationItem) {
            nameTextView.text = item.medication.name
            doseTextView.text = item.medication.dose
            scheduleTextView.text = item.scheduleSummary

            val photoUri = item.medication.photoUri
            val bitmap = photoUri?.takeIf { it.isNotBlank() }?.let { fileStorageHelper.loadPhoto(it) }
            if (bitmap != null) {
                thumbnailImageView.setImageBitmap(bitmap)
            } else {
                thumbnailImageView.setImageResource(R.drawable.ic_lucide_image)
            }

            StatusChipBinder.bind(statusChip, item.todayStatus)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<SeniorMedicationItem>() {
        override fun areItemsTheSame(oldItem: SeniorMedicationItem, newItem: SeniorMedicationItem) =
            oldItem.medication.id == newItem.medication.id

        override fun areContentsTheSame(oldItem: SeniorMedicationItem, newItem: SeniorMedicationItem) =
            oldItem == newItem
    }
}
