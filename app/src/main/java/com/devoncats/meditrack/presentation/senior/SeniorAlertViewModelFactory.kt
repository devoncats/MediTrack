package com.devoncats.meditrack.presentation.senior

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.data.repository.MedicationRepositoryImpl
import com.devoncats.meditrack.domain.usecase.ConfirmDoseUseCase
import com.devoncats.meditrack.services.AlarmScheduler

class SeniorAlertViewModelFactory(context: Context, private val scheduleId: Long) : ViewModelProvider.Factory {

    private val appContext = context.applicationContext
    private val medicationRepository = MedicationRepositoryImpl(
        MediTrackDatabase.getInstance(appContext).medicationDao(),
        MediTrackDatabase.getInstance(appContext).scheduleDao(),
        MediTrackDatabase.getInstance(appContext).medicationLogDao()
    )
    private val alarmScheduler = AlarmScheduler(appContext)
    private val confirmDoseUseCase = ConfirmDoseUseCase(medicationRepository, alarmScheduler)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SeniorAlertViewModel(medicationRepository, confirmDoseUseCase, scheduleId) as T
}
