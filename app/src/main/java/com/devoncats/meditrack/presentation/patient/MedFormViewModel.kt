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
import java.time.DayOfWeek
import java.time.LocalTime
import kotlinx.coroutines.launch

sealed class MedFormSaveResult {
    data object Success : MedFormSaveResult()
    data object ValidationError : MedFormSaveResult()
}

class MedFormViewModel(
    private val medicationRepository: MedicationRepository,
    private val alarmScheduler: AlarmScheduler,
    private val ownerUserId: Long
) : ViewModel() {

    private val _saveResult = MutableLiveData<MedFormSaveResult>()
    val saveResult: LiveData<MedFormSaveResult> = _saveResult

    fun saveNewMedication(
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
            val medicationId = medicationRepository.insertMedication(
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

            val daysOfWeek = selectedDays.joinToString(",") { it.toCode() }
            selectedTimes.forEach { time ->
                val timeString = "%02d:%02d".format(time.hour, time.minute)
                val scheduleId = medicationRepository.insertSchedule(
                    Schedule(id = 0, medicationId = medicationId, time = timeString, daysOfWeek = daysOfWeek)
                )
                alarmScheduler.schedule(scheduleId, medicationId, timeString, daysOfWeek)
            }

            _saveResult.value = MedFormSaveResult.Success
        }
    }
}
