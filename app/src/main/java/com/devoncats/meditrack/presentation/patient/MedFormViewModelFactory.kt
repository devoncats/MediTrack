package com.devoncats.meditrack.presentation.patient

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.data.local.SessionManager
import com.devoncats.meditrack.data.repository.MedicationRepositoryImpl
import com.devoncats.meditrack.domain.usecase.SaveMedicationUseCase
import com.devoncats.meditrack.services.AlarmScheduler
import com.devoncats.meditrack.services.FileStorageHelper

class MedFormViewModelFactory(
    context: Context,
    private val medicationId: Long,
    private val seniorUserId: Long = NO_SENIOR_USER_ID
) : ViewModelProvider.Factory {

    private val appContext = context.applicationContext
    private val medicationRepository = MedicationRepositoryImpl(
        MediTrackDatabase.getInstance(appContext).medicationDao(),
        MediTrackDatabase.getInstance(appContext).scheduleDao(),
        MediTrackDatabase.getInstance(appContext).medicationLogDao()
    )
    private val alarmScheduler = AlarmScheduler(appContext)
    private val fileStorageHelper = FileStorageHelper(appContext)
    private val saveMedicationUseCase = SaveMedicationUseCase(medicationRepository, alarmScheduler, fileStorageHelper)
    private val ownerUserId = if (seniorUserId != NO_SENIOR_USER_ID) {
        seniorUserId
    } else {
        SessionManager(appContext).getUserId()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        MedFormViewModel(medicationRepository, saveMedicationUseCase, ownerUserId, medicationId) as T

    companion object {
        const val NO_SENIOR_USER_ID = -1L
    }
}
