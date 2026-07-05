package com.devoncats.meditrack.presentation.caregiver

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.devoncats.meditrack.R
import com.google.android.material.button.MaterialButton

class MissedDoseAlertFragment : Fragment(R.layout.fragment_missed_dose_alert) {

    private val logId: Long
        get() = requireArguments().getLong("logId")

    private val viewModel: MissedDoseAlertViewModel by viewModels {
        MissedDoseAlertViewModelFactory(requireContext(), logId)
    }

    private var phoneToCall: String? = null

    private val requestCallPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) placeCall()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val seniorNameTextView = view.findViewById<TextView>(R.id.missedDoseSeniorName)
        val medicationNameTextView = view.findViewById<TextView>(R.id.missedDoseMedicationName)
        val scheduledTimeTextView = view.findViewById<TextView>(R.id.missedDoseScheduledTime)
        val noContactMessageTextView = view.findViewById<View>(R.id.missedDoseNoContactMessage)
        val callButton = view.findViewById<MaterialButton>(R.id.callEmergencyContactButton)
        val dismissButton = view.findViewById<MaterialButton>(R.id.dismissButton)

        viewModel.alertInfo.observe(viewLifecycleOwner) { info ->
            seniorNameTextView.text = info.seniorName
            medicationNameTextView.text = info.medicationName
            scheduledTimeTextView.text = info.scheduledTime

            phoneToCall = info.emergencyContactPhone
            val hasContact = !info.emergencyContactPhone.isNullOrBlank()
            callButton.isEnabled = hasContact
            noContactMessageTextView.visibility = if (hasContact) View.GONE else View.VISIBLE
        }

        callButton.setOnClickListener { requestCallPermissionAndCall() }
        dismissButton.setOnClickListener { findNavController().popBackStack() }
    }

    private fun requestCallPermissionAndCall() {
        if (phoneToCall.isNullOrBlank()) return

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            placeCall()
        } else {
            requestCallPermission.launch(Manifest.permission.CALL_PHONE)
        }
    }

    private fun placeCall() {
        val phone = phoneToCall ?: return
        startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$phone")))
    }
}
