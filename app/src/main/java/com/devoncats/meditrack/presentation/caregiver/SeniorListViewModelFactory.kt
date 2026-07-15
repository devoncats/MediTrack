package com.devoncats.meditrack.presentation.caregiver

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.data.local.SessionManager
import com.devoncats.meditrack.data.repository.MedicationRepositoryImpl
import com.devoncats.meditrack.data.repository.UserRepositoryImpl
import com.devoncats.meditrack.domain.usecase.DeleteMedicationUseCase
import com.devoncats.meditrack.domain.usecase.DeleteSeniorUseCase
import com.devoncats.meditrack.services.AlarmScheduler
import com.devoncats.meditrack.services.FileStorageHelper

class SeniorListViewModelFactory(context: Context) : ViewModelProvider.Factory {

    private val appContext = context.applicationContext
    private val userRepository = UserRepositoryImpl(MediTrackDatabase.getInstance(appContext).userDao())
    private val medicationRepository = MedicationRepositoryImpl(
        MediTrackDatabase.getInstance(appContext).medicationDao(),
        MediTrackDatabase.getInstance(appContext).scheduleDao(),
        MediTrackDatabase.getInstance(appContext).medicationLogDao()
    )
    private val alarmScheduler = AlarmScheduler(appContext)
    private val fileStorageHelper = FileStorageHelper(appContext)
    private val deleteMedicationUseCase = DeleteMedicationUseCase(medicationRepository, alarmScheduler, fileStorageHelper)
    private val deleteSeniorUseCase = DeleteSeniorUseCase(userRepository, medicationRepository, deleteMedicationUseCase)
    private val caregiverId = SessionManager(appContext).getUserId()

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SeniorListViewModel(userRepository, medicationRepository, deleteSeniorUseCase, caregiverId) as T
}
