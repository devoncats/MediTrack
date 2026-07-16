package com.devoncats.meditrack.presentation.senior

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
class SeniorAlertFragment : Fragment(R.layout.fragment_senior_alert) {

    private val viewModel: SeniorAlertViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val medicationNameTextView = view.findViewById<TextView>(R.id.seniorAlertMedicationName)
        val doseTextView = view.findViewById<TextView>(R.id.seniorAlertDose)
        val scheduledTimeTextView = view.findViewById<TextView>(R.id.seniorAlertScheduledTime)
        val confirmButton = view.findViewById<MaterialButton>(R.id.seniorAlertConfirmButton)

        viewModel.alertInfo.observe(viewLifecycleOwner) { info ->
            if (info == null) return@observe
            medicationNameTextView.text = info.medicationName
            doseTextView.text = info.dose
            scheduledTimeTextView.text = info.scheduledTime
        }

        confirmButton.setOnClickListener { viewModel.confirm() }

        viewModel.closeScreen.observe(viewLifecycleOwner) {
            findNavController().popBackStack()
        }
    }
}
