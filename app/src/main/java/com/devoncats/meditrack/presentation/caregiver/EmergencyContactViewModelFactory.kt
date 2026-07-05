package com.devoncats.meditrack.presentation.caregiver

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.data.repository.EmergencyContactRepositoryImpl

class EmergencyContactViewModelFactory(context: Context, private val seniorUserId: Long) : ViewModelProvider.Factory {

    private val appContext = context.applicationContext
    private val emergencyContactRepository = EmergencyContactRepositoryImpl(
        MediTrackDatabase.getInstance(appContext).emergencyContactDao()
    )

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        EmergencyContactViewModel(emergencyContactRepository, seniorUserId) as T
}
