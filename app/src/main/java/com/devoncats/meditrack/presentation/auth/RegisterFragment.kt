package com.devoncats.meditrack.presentation.auth

import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.devoncats.meditrack.R
import com.devoncats.meditrack.domain.model.UserRole
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private val viewModel: RegisterViewModel by viewModels {
        RegisterViewModelFactory(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nameEditText = view.findViewById<TextInputEditText>(R.id.nameEditText)
        val emailEditText = view.findViewById<TextInputEditText>(R.id.emailEditText)
        val passwordEditText = view.findViewById<TextInputEditText>(R.id.passwordEditText)
        val roleRadioGroup = view.findViewById<RadioGroup>(R.id.roleRadioGroup)
        val registerButton = view.findViewById<MaterialButton>(R.id.registerButton)
        val errorTextView = view.findViewById<TextView>(R.id.errorTextView)
        val loginLinkTextView = view.findViewById<View>(R.id.loginLinkTextView)

        fun updateButtonState() {
            registerButton.isEnabled = !nameEditText.text.isNullOrBlank() &&
                !emailEditText.text.isNullOrBlank() &&
                !passwordEditText.text.isNullOrBlank() &&
                roleRadioGroup.checkedRadioButtonId != -1
        }

        nameEditText.doAfterTextChanged { updateButtonState() }
        emailEditText.doAfterTextChanged { updateButtonState() }
        passwordEditText.doAfterTextChanged { updateButtonState() }
        roleRadioGroup.setOnCheckedChangeListener { _, _ -> updateButtonState() }

        registerButton.setOnClickListener {
            errorTextView.visibility = View.GONE
            // Explicit per id rather than an else-defaults-to-CAREGIVER fallback: the button
            // is disabled until a radio is checked, but this way an unrecognized id bails out
            // instead of silently registering the wrong role.
            val role = when (roleRadioGroup.checkedRadioButtonId) {
                R.id.patientRadioButton -> UserRole.PATIENT
                R.id.caregiverRadioButton -> UserRole.CAREGIVER
                else -> return@setOnClickListener
            }
            viewModel.register(
                name = nameEditText.text.toString().trim(),
                email = emailEditText.text.toString().trim(),
                password = passwordEditText.text.toString(),
                role = role
            )
        }

        loginLinkTextView.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }

        viewModel.registerResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                RegisterResult.Success -> findNavController().navigate(R.id.action_register_to_login)
                RegisterResult.InvalidEmailFormat -> showError(errorTextView, R.string.register_error_invalid_email)
                RegisterResult.EmailAlreadyRegistered -> showError(errorTextView, R.string.register_error_email_taken)
            }
        }
    }

    private fun showError(errorTextView: TextView, messageRes: Int) {
        errorTextView.setText(messageRes)
        errorTextView.visibility = View.VISIBLE
    }
}
