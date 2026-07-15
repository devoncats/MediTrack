package com.devoncats.meditrack.presentation.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.data.local.SessionManager
import com.devoncats.meditrack.data.repository.UserRepositoryImpl
import com.devoncats.meditrack.domain.usecase.LoginUseCase

class LoginViewModelFactory(context: Context) : ViewModelProvider.Factory {

    private val appContext = context.applicationContext
    private val userRepository = UserRepositoryImpl(MediTrackDatabase.getInstance(appContext).userDao())
    private val sessionManager = SessionManager(appContext)
    private val loginUseCase = LoginUseCase(userRepository, sessionManager)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        LoginViewModel(loginUseCase) as T
}
