package com.devoncats.meditrack.presentation.auth

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devoncats.meditrack.domain.model.User
import com.devoncats.meditrack.domain.model.UserRole
import com.devoncats.meditrack.domain.repository.UserRepository
import com.devoncats.meditrack.utils.PasswordHasher
import kotlinx.coroutines.launch

sealed class RegisterResult {
    data object Success : RegisterResult()
    data object InvalidEmailFormat : RegisterResult()
    data object EmailAlreadyRegistered : RegisterResult()
}

class RegisterViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _registerResult = MutableLiveData<RegisterResult>()
    val registerResult: LiveData<RegisterResult> = _registerResult

    fun register(name: String, email: String, password: String, role: UserRole) {
        viewModelScope.launch {
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                _registerResult.value = RegisterResult.InvalidEmailFormat
                return@launch
            }
            if (userRepository.findByEmail(email) != null) {
                _registerResult.value = RegisterResult.EmailAlreadyRegistered
                return@launch
            }
            userRepository.insert(
                User(
                    id = 0,
                    name = name,
                    email = email,
                    passwordHash = PasswordHasher.hash(password),
                    role = role,
                    caregiverId = null
                )
            )
            _registerResult.value = RegisterResult.Success
        }
    }
}
