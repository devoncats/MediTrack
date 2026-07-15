package com.devoncats.meditrack.presentation.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.data.repository.UserRepositoryImpl
import com.devoncats.meditrack.domain.usecase.RegisterUserUseCase

class RegisterViewModelFactory(context: Context) : ViewModelProvider.Factory {

    private val appContext = context.applicationContext
    private val userRepository = UserRepositoryImpl(MediTrackDatabase.getInstance(appContext).userDao())
    private val registerUserUseCase = RegisterUserUseCase(userRepository)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        RegisterViewModel(registerUserUseCase) as T
}
