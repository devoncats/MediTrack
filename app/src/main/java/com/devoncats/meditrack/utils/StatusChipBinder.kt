package com.devoncats.meditrack.utils

import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import com.devoncats.meditrack.R
import com.devoncats.meditrack.domain.model.MedicationLogStatus
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors

/**
 * Binds a status chip's text, icon, and color consistently across the medication list,
 * senior's read-only list, and senior list adapters (color is never the only signal: text
 * and icon are always present too, except for the neutral "nothing scheduled" state).
 */
object StatusChipBinder {

    fun bind(chip: Chip, status: MedicationLogStatus?, noDosesTextRes: Int? = null) {
        val context = chip.context

        if (status == null && noDosesTextRes != null) {
            val onSurfaceVariant = MaterialColors.getColor(chip, com.google.android.material.R.attr.colorOnSurfaceVariant)
            val surfaceVariant = MaterialColors.getColor(chip, com.google.android.material.R.attr.colorSurfaceVariant)
            chip.setText(noDosesTextRes)
            chip.isChipIconVisible = false
            chip.chipBackgroundColor = ColorStateList.valueOf(surfaceVariant)
            chip.setTextColor(onSurfaceVariant)
            chip.chipStrokeWidth = 0f
            return
        }

        val (textRes, iconRes, containerColorRes, onContainerColorRes) = when (status) {
            MedicationLogStatus.CONFIRMED -> StatusStyle(
                R.string.med_status_confirmed,
                R.drawable.ic_lucide_check_circle,
                R.color.status_confirmed_container,
                R.color.status_confirmed_on_container
            )
            MedicationLogStatus.MISSED -> StatusStyle(
                R.string.med_status_missed,
                R.drawable.ic_lucide_alert_triangle,
                R.color.status_missed_container,
                R.color.status_missed_on_container
            )
            MedicationLogStatus.PENDING, null -> StatusStyle(
                R.string.med_status_pending,
                R.drawable.ic_lucide_clock,
                R.color.status_pending_container,
                R.color.status_pending_on_container
            )
        }

        chip.setText(textRes)
        chip.chipIcon = ContextCompat.getDrawable(context, iconRes)
        chip.isChipIconVisible = true
        val onContainerColor = ContextCompat.getColor(context, onContainerColorRes)
        chip.chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(context, containerColorRes))
        chip.setTextColor(onContainerColor)
        chip.chipIconTint = ColorStateList.valueOf(onContainerColor)
        chip.chipStrokeWidth = 0f
    }

    private data class StatusStyle(
        val textRes: Int,
        val iconRes: Int,
        val containerColorRes: Int,
        val onContainerColorRes: Int
    )
}
