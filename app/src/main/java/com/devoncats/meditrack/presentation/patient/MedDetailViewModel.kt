package com.devoncats.meditrack.presentation.patient

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.devoncats.meditrack.domain.model.Medication
import com.devoncats.meditrack.domain.model.MedicationLog
import com.devoncats.meditrack.domain.repository.MedicationRepository
import com.devoncats.meditrack.services.AlarmScheduler
import com.devoncats.meditrack.utils.DateUtils
import kotlinx.coroutines.launch

class MedDetailViewModel(
    private val medicationRepository: MedicationRepository,
    private val alarmScheduler: AlarmScheduler,
    private val medicationId: Long
) : ViewModel() {

    val medication: LiveData<Medication?> = medicationRepository.observeMedicationById(medicationId)

    val todayLogs: LiveData<List<MedicationLog>> = medicationRepository.observeLogsByMedication(medicationId)
        .map { logs ->
            val (startInclusive, endExclusive) = DateUtils.todayRangeMillis()
            logs.filter { it.scheduledDatetime in startInclusive until endExclusive }
                .sortedBy { it.scheduledDatetime }
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
            _deleted.value = true
        }
    }
}
