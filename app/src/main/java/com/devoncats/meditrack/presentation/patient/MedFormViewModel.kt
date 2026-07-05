package com.devoncats.meditrack.presentation.patient

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devoncats.meditrack.domain.model.Medication
import com.devoncats.meditrack.domain.model.Schedule
import com.devoncats.meditrack.domain.repository.MedicationRepository
import com.devoncats.meditrack.services.AlarmScheduler
import com.devoncats.meditrack.utils.toCode
import com.devoncats.meditrack.utils.toDayOfWeekOrNull
import java.time.DayOfWeek
import java.time.LocalTime
import kotlinx.coroutines.launch

sealed class MedFormSaveResult {
    data object Success : MedFormSaveResult()
    data object ValidationError : MedFormSaveResult()
}

data class MedFormPrefill(
    val name: String,
    val dose: String,
    val frequency: String,
    val instructions: String?,
    val days: Set<DayOfWeek>,
    val times: List<LocalTime>
)

class MedFormViewModel(
    private val medicationRepository: MedicationRepository,
    private val alarmScheduler: AlarmScheduler,
    private val ownerUserId: Long,
    private val medicationId: Long
) : ViewModel() {

    val isEditMode: Boolean = medicationId != NEW_MEDICATION_ID

    private val _prefill = MutableLiveData<MedFormPrefill>()
    val prefill: LiveData<MedFormPrefill> = _prefill

    private val _saveResult = MutableLiveData<MedFormSaveResult>()
    val saveResult: LiveData<MedFormSaveResult> = _saveResult

    init {
        if (isEditMode) {
            viewModelScope.launch {
                val medication = medicationRepository.getMedicationById(medicationId) ?: return@launch
                val schedules = medicationRepository.getSchedulesByMedication(medicationId)
                val days = schedules.firstOrNull()?.daysOfWeek
                    ?.split(",")
                    ?.mapNotNull { it.toDayOfWeekOrNull() }
                    ?.toSet()
                    .orEmpty()
                val times = schedules.mapNotNull { runCatching { LocalTime.parse(it.time) }.getOrNull() }

                _prefill.value = MedFormPrefill(
                    name = medication.name,
                    dose = medication.dose,
                    frequency = medication.frequency,
                    instructions = medication.instructions,
                    days = days,
                    times = times
                )
            }
        }
    }

    fun save(
        name: String,
        dose: String,
        frequency: String,
        instructions: String?,
        selectedDays: Set<DayOfWeek>,
        selectedTimes: List<LocalTime>
    ) {
        if (name.isBlank() || dose.isBlank() || frequency.isBlank() || selectedDays.isEmpty() || selectedTimes.isEmpty()) {
            _saveResult.value = MedFormSaveResult.ValidationError
            return
        }

        viewModelScope.launch {
            val resolvedMedicationId = if (isEditMode) {
                medicationRepository.updateMedication(
                    Medication(
                        id = medicationId,
                        name = name,
                        dose = dose,
                        frequency = frequency,
                        instructions = instructions?.takeIf { it.isNotBlank() },
                        ownerUserId = ownerUserId,
                        photoUri = null,
                        createdAt = System.currentTimeMillis()
                    )
                )
                medicationId
            } else {
                medicationRepository.insertMedication(
                    Medication(
                        id = 0,
                        name = name,
                        dose = dose,
                        frequency = frequency,
                        instructions = instructions?.takeIf { it.isNotBlank() },
                        ownerUserId = ownerUserId,
                        photoUri = null,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            if (isEditMode) {
                medicationRepository.getSchedulesByMedication(resolvedMedicationId).forEach { oldSchedule ->
                    alarmScheduler.cancel(oldSchedule.id)
                    medicationRepository.deleteSchedule(oldSchedule)
                }
            }

            val daysOfWeek = selectedDays.joinToString(",") { it.toCode() }
            selectedTimes.forEach { time ->
                val timeString = "%02d:%02d".format(time.hour, time.minute)
                val scheduleId = medicationRepository.insertSchedule(
                    Schedule(id = 0, medicationId = resolvedMedicationId, time = timeString, daysOfWeek = daysOfWeek)
                )
                alarmScheduler.schedule(scheduleId, resolvedMedicationId, timeString, daysOfWeek)
            }

            _saveResult.value = MedFormSaveResult.Success
        }
    }

    companion object {
        const val NEW_MEDICATION_ID = -1L
    }
}
