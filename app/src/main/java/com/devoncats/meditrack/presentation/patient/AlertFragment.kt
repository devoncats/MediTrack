package com.devoncats.meditrack.presentation.patient

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.devoncats.meditrack.R
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AlertFragment : Fragment(R.layout.fragment_alert) {

    private val viewModel: AlertViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val medicationNameTextView = view.findViewById<TextView>(R.id.alertMedicationName)
        val doseTextView = view.findViewById<TextView>(R.id.alertDose)
        val scheduledTimeTextView = view.findViewById<TextView>(R.id.alertScheduledTime)
        val confirmButton = view.findViewById<MaterialButton>(R.id.confirmButton)
        val postponeButton = view.findViewById<MaterialButton>(R.id.postponeButton)
        val dismissButton = view.findViewById<MaterialButton>(R.id.dismissButton)

        viewModel.alertInfo.observe(viewLifecycleOwner) { info ->
            if (info == null) return@observe
            medicationNameTextView.text = info.medicationName
            doseTextView.text = getString(R.string.notification_medication_alarm_text, info.dose)
            scheduledTimeTextView.text = info.scheduledTime
        }

        confirmButton.setOnClickListener { viewModel.confirm() }
        postponeButton.setOnClickListener { viewModel.postpone() }
        dismissButton.setOnClickListener { viewModel.dismiss() }

        viewModel.closeScreen.observe(viewLifecycleOwner) {
            findNavController().popBackStack()
        }
    }
}
