package com.devoncats.meditrack.presentation.auth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.devoncats.meditrack.R
import com.devoncats.meditrack.domain.model.UserRole
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginFragment : Fragment(R.layout.fragment_login) {

    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val emailInputLayout = view.findViewById<TextInputLayout>(R.id.emailInputLayout)
        val emailEditText = view.findViewById<TextInputEditText>(R.id.emailEditText)
        val passwordEditText = view.findViewById<TextInputEditText>(R.id.passwordEditText)
        val loginButton = view.findViewById<MaterialButton>(R.id.loginButton)
        val errorTextView = view.findViewById<View>(R.id.errorTextView)
        val registerLinkTextView = view.findViewById<View>(R.id.registerLinkTextView)

        val fieldsWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                loginButton.isEnabled = !emailEditText.text.isNullOrBlank() &&
                    !passwordEditText.text.isNullOrBlank()
            }
        }
        emailEditText.addTextChangedListener(fieldsWatcher)
        passwordEditText.addTextChangedListener(fieldsWatcher)

        loginButton.setOnClickListener {
            errorTextView.visibility = View.GONE
            emailInputLayout.error = null
            viewModel.login(
                email = emailEditText.text.toString().trim(),
                password = passwordEditText.text.toString()
            )
        }

        registerLinkTextView.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        viewModel.loginResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is LoginResult.Success -> navigateToRoleGraph(result.role)
                LoginResult.InvalidCredentials -> {
                    errorTextView.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun navigateToRoleGraph(role: UserRole) {
        val actionId = when (role) {
            UserRole.PATIENT -> R.id.action_login_to_patientGraph
            UserRole.CAREGIVER -> R.id.action_login_to_caregiverGraph
            UserRole.SENIOR_PATIENT -> R.id.action_login_to_seniorGraph
        }
        findNavController().navigate(actionId)
    }
}
