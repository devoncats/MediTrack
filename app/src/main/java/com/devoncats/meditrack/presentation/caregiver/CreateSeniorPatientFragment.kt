package com.devoncats.meditrack.presentation.caregiver

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.devoncats.meditrack.R
import com.devoncats.meditrack.domain.usecase.CreateSeniorPatientResult
import com.devoncats.meditrack.domain.usecase.GeneratedCredentials
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateSeniorPatientFragment : Fragment(R.layout.fragment_create_senior_patient) {

    private val viewModel: CreateSeniorPatientViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nameEditText = view.findViewById<TextInputEditText>(R.id.nameEditText)
        val contactNameEditText = view.findViewById<TextInputEditText>(R.id.contactNameEditText)
        val contactPhoneEditText = view.findViewById<TextInputEditText>(R.id.contactPhoneEditText)
        val generateButton = view.findViewById<MaterialButton>(R.id.generateButton)

        nameEditText.doAfterTextChanged {
            generateButton.isEnabled = !nameEditText.text.isNullOrBlank()
        }

        generateButton.setOnClickListener {
            viewModel.createSeniorPatient(
                nameEditText.text.toString().trim(),
                contactNameEditText.text.toString().trim(),
                contactPhoneEditText.text.toString().trim()
            )
        }

        viewModel.result.observe(viewLifecycleOwner) { result ->
            when (result) {
                is CreateSeniorPatientResult.Success -> showCredentialsDialog(result.credentials)
                CreateSeniorPatientResult.ValidationError -> Unit
            }
        }
    }

    private fun showCredentialsDialog(credentials: GeneratedCredentials) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.create_senior_dialog_title)
            .setMessage(
                getString(R.string.create_senior_dialog_message, credentials.username, credentials.pin)
            )
            .setCancelable(false)
            .setPositiveButton(R.string.create_senior_dialog_positive) { _, _ ->
                findNavController().popBackStack()
            }
            .show()
    }
}
