package com.devoncats.meditrack.presentation.caregiver

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.data.local.SessionManager
import com.devoncats.meditrack.data.repository.UserRepositoryImpl

class CreateSeniorPatientViewModelFactory(context: Context) : ViewModelProvider.Factory {

    private val appContext = context.applicationContext
    private val userRepository = UserRepositoryImpl(MediTrackDatabase.getInstance(appContext).userDao())
    private val caregiverId = SessionManager(appContext).getUserId()

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CreateSeniorPatientViewModel(userRepository, caregiverId) as T
}
