package com.devoncats.meditrack.presentation.patient

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.data.repository.MedicationRepositoryImpl
import com.devoncats.meditrack.domain.usecase.DeleteMedicationUseCase
import com.devoncats.meditrack.services.AlarmScheduler
import com.devoncats.meditrack.services.FileStorageHelper

class MedDetailViewModelFactory(context: Context, private val medicationId: Long) : ViewModelProvider.Factory {

    private val appContext = context.applicationContext
    private val medicationRepository = MedicationRepositoryImpl(
        MediTrackDatabase.getInstance(appContext).medicationDao(),
        MediTrackDatabase.getInstance(appContext).scheduleDao(),
        MediTrackDatabase.getInstance(appContext).medicationLogDao()
    )
    private val alarmScheduler = AlarmScheduler(appContext)
    private val fileStorageHelper = FileStorageHelper(appContext)
    private val deleteMedicationUseCase = DeleteMedicationUseCase(medicationRepository, alarmScheduler, fileStorageHelper)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        MedDetailViewModel(medicationRepository, deleteMedicationUseCase, medicationId) as T
}
