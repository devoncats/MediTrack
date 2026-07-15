package com.devoncats.meditrack.presentation.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devoncats.meditrack.domain.model.UserRole
import com.devoncats.meditrack.domain.usecase.RegisterResult
import com.devoncats.meditrack.domain.usecase.RegisterUserUseCase
import kotlinx.coroutines.launch

class RegisterViewModel(private val registerUserUseCase: RegisterUserUseCase) : ViewModel() {

    private val _registerResult = MutableLiveData<RegisterResult>()
    val registerResult: LiveData<RegisterResult> = _registerResult

    fun register(name: String, email: String, password: String, role: UserRole) {
        viewModelScope.launch {
            _registerResult.value = registerUserUseCase(name, email, password, role)
        }
    }
}
