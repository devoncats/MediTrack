package com.devoncats.meditrack.presentation.caregiver

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.data.repository.EmergencyContactRepositoryImpl
import com.devoncats.meditrack.data.repository.MedicationRepositoryImpl
import com.devoncats.meditrack.data.repository.UserRepositoryImpl

class MissedDoseAlertViewModelFactory(context: Context, private val logId: Long) : ViewModelProvider.Factory {

    private val appContext = context.applicationContext
    private val medicationRepository = MedicationRepositoryImpl(
        MediTrackDatabase.getInstance(appContext).medicationDao(),
        MediTrackDatabase.getInstance(appContext).scheduleDao(),
        MediTrackDatabase.getInstance(appContext).medicationLogDao()
    )
    private val userRepository = UserRepositoryImpl(MediTrackDatabase.getInstance(appContext).userDao())
    private val emergencyContactRepository = EmergencyContactRepositoryImpl(
        MediTrackDatabase.getInstance(appContext).emergencyContactDao()
    )

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        MissedDoseAlertViewModel(medicationRepository, userRepository, emergencyContactRepository, logId) as T
}
