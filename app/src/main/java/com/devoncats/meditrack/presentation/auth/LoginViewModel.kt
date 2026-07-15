package com.devoncats.meditrack.presentation.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devoncats.meditrack.data.local.SessionManager
import com.devoncats.meditrack.domain.model.UserRole
import com.devoncats.meditrack.domain.repository.UserRepository
import com.devoncats.meditrack.utils.PasswordHasher
import kotlinx.coroutines.launch

sealed class LoginResult {
    data class Success(val role: UserRole) : LoginResult()
    data object InvalidCredentials : LoginResult()
}

class LoginViewModel(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    fun login(email: String, password: String) {
        viewModelScope.launch {
            val user = userRepository.findByUsername(email)
            if (user == null || !PasswordHasher.verify(password, user.passwordHash)) {
                _loginResult.value = LoginResult.InvalidCredentials
                return@launch
            }
            sessionManager.saveSession(user.id, user.role.name)
            _loginResult.value = LoginResult.Success(user.role)
        }
    }
}
