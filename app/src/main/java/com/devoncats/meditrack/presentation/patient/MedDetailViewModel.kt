package com.devoncats.meditrack.presentation.patient

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.devoncats.meditrack.domain.model.Medication
import com.devoncats.meditrack.domain.model.MedicationLog
import com.devoncats.meditrack.domain.repository.MedicationRepository
import com.devoncats.meditrack.services.AlarmScheduler
import com.devoncats.meditrack.services.FileStorageHelper
import com.devoncats.meditrack.utils.DateUtils
import kotlinx.coroutines.launch

class MedDetailViewModel(
    private val medicationRepository: MedicationRepository,
    private val alarmScheduler: AlarmScheduler,
    private val fileStorageHelper: FileStorageHelper,
    private val medicationId: Long
) : ViewModel() {

    val medication: LiveData<Medication?> = medicationRepository.observeMedicationById(medicationId)

    private val todayRange = MutableLiveData(DateUtils.todayRangeMillis())

    // Re-reads the current day boundaries; called from the fragment's onResume so "today's
    // history" doesn't stay pinned to whatever day the ViewModel happened to be created on.
    fun refreshTodayRange() {
        val current = DateUtils.todayRangeMillis()
        if (todayRange.value != current) todayRange.value = current
    }

    // Filtered by query (same criterion as the other list ViewModels) rather than fetching
    // every log for this medication and filtering "today" in memory.
    val todayLogs: LiveData<List<MedicationLog>> = todayRange.switchMap { (startInclusive, endExclusive) ->
        medicationRepository.observeLogsByMedicationBetween(medicationId, startInclusive, endExclusive)
            .map { logs -> logs.sortedBy { it.scheduledDatetime } }
    }

    private val _deleted = MutableLiveData<Boolean>()
    val deleted: LiveData<Boolean> = _deleted

    fun deleteMedication() {
        viewModelScope.launch {
            val medication = medicationRepository.getMedicationById(medicationId) ?: return@launch
            medicationRepository.getSchedulesByMedication(medicationId).forEach { schedule ->
                alarmScheduler.cancel(schedule.id)
            }
            medicationRepository.deleteMedication(medication)
            medication.photoUri?.takeIf { it.isNotBlank() }?.let { fileStorageHelper.deletePhoto(it) }
            _deleted.value = true
        }
    }
}
