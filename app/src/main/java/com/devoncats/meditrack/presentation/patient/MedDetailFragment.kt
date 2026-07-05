package com.devoncats.meditrack.presentation.patient

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.devoncats.meditrack.R
import com.devoncats.meditrack.domain.model.MedicationLog
import com.devoncats.meditrack.domain.model.MedicationLogStatus
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MedDetailFragment : Fragment(R.layout.fragment_med_detail) {

    private val medicationId: Long
        get() = arguments?.getLong("medicationId", -1L) ?: -1L

    private val viewModel: MedDetailViewModel by viewModels {
        MedDetailViewModelFactory(requireContext(), medicationId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val photoImageView = view.findViewById<ImageView>(R.id.medicationPhoto)
        val nameTextView = view.findViewById<TextView>(R.id.medicationName)
        val doseTextView = view.findViewById<TextView>(R.id.medicationDose)
        val frequencyTextView = view.findViewById<TextView>(R.id.medicationFrequency)
        val instructionsTextView = view.findViewById<TextView>(R.id.medicationInstructions)
        val todayHistoryEmptyTextView = view.findViewById<View>(R.id.todayHistoryEmptyTextView)
        val todayHistoryContainer = view.findViewById<LinearLayout>(R.id.todayHistoryContainer)
        val editButton = view.findViewById<MaterialButton>(R.id.editButton)
        val deleteButton = view.findViewById<MaterialButton>(R.id.deleteButton)

        viewModel.medication.observe(viewLifecycleOwner) { medication ->
            if (medication == null) return@observe
            nameTextView.text = medication.name
            doseTextView.text = medication.dose
            frequencyTextView.text = medication.frequency
            if (medication.instructions.isNullOrBlank()) {
                instructionsTextView.visibility = View.GONE
            } else {
                instructionsTextView.text = medication.instructions
                instructionsTextView.visibility = View.VISIBLE
            }
            photoImageView.visibility = if (medication.photoUri.isNullOrBlank()) View.GONE else View.VISIBLE

            editButton.setOnClickListener {
                findNavController().navigate(
                    R.id.action_medDetail_to_medForm,
                    bundleOf("medicationId" to medication.id)
                )
            }

            deleteButton.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.med_detail_delete_confirm_title)
                    .setMessage(getString(R.string.med_detail_delete_confirm_message, medication.name))
                    .setPositiveButton(R.string.med_detail_delete_confirm_positive) { _, _ ->
                        viewModel.deleteMedication()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }

        viewModel.todayLogs.observe(viewLifecycleOwner) { logs ->
            renderTodayLogs(todayHistoryContainer, logs)
            todayHistoryEmptyTextView.visibility = if (logs.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.deleted.observe(viewLifecycleOwner) { deleted ->
            if (deleted) findNavController().popBackStack()
        }
    }

    private fun renderTodayLogs(container: LinearLayout, logs: List<MedicationLog>) {
        container.removeAllViews()
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        logs.forEach { log ->
            val row = LayoutInflater.from(requireContext()).inflate(R.layout.item_medication_log, container, false)
            val time = Instant.ofEpochMilli(log.scheduledDatetime).atZone(ZoneId.systemDefault()).toLocalTime()
            row.findViewById<TextView>(R.id.logTime).text = time.format(timeFormatter)
            row.findViewById<Chip>(R.id.logStatusChip).setText(
                when (log.status) {
                    MedicationLogStatus.CONFIRMED -> R.string.med_status_confirmed
                    MedicationLogStatus.PENDING -> R.string.med_status_pending
                    MedicationLogStatus.MISSED -> R.string.med_status_missed
                }
            )
            container.addView(row)
        }
    }
}
