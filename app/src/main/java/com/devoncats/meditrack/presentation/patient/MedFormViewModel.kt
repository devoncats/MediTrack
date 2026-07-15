package com.devoncats.meditrack.presentation.patient

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devoncats.meditrack.domain.model.Medication
import com.devoncats.meditrack.domain.model.Schedule
import com.devoncats.meditrack.domain.model.WeekDays
import com.devoncats.meditrack.domain.repository.MedicationRepository
import com.devoncats.meditrack.services.AlarmScheduler
import com.devoncats.meditrack.services.FileStorageHelper
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
    val times: List<LocalTime>,
    val photoUri: String?
)

class MedFormViewModel(
    private val medicationRepository: MedicationRepository,
    private val alarmScheduler: AlarmScheduler,
    private val fileStorageHelper: FileStorageHelper,
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
                val days = schedules.firstOrNull()?.daysOfWeek?.days.orEmpty()
                val times = schedules.map { it.time }

                _prefill.value = MedFormPrefill(
                    name = medication.name,
                    dose = medication.dose,
                    frequency = medication.frequency,
                    instructions = medication.instructions,
                    days = days,
                    times = times,
                    photoUri = medication.photoUri
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
        selectedTimes: List<LocalTime>,
        capturedPhotoUri: Uri?
    ) {
        if (name.isBlank() || dose.isBlank() || frequency.isBlank() || selectedDays.isEmpty() || selectedTimes.isEmpty()) {
            _saveResult.value = MedFormSaveResult.ValidationError
            return
        }

        viewModelScope.launch {
            val existingMedication = if (isEditMode) medicationRepository.getMedicationById(medicationId) else null
            val photoUriToPersist = if (capturedPhotoUri != null) {
                existingMedication?.photoUri?.takeIf { it.isNotBlank() }?.let { fileStorageHelper.deletePhoto(it) }
                fileStorageHelper.savePhoto(capturedPhotoUri)
            } else {
                existingMedication?.photoUri
            }

            val resolvedMedicationId = if (isEditMode) {
                medicationRepository.updateMedication(
                    Medication(
                        id = medicationId,
                        name = name,
                        dose = dose,
                        frequency = frequency,
                        instructions = instructions?.takeIf { it.isNotBlank() },
                        ownerUserId = ownerUserId,
                        photoUri = photoUriToPersist,
                        createdAt = existingMedication?.createdAt ?: System.currentTimeMillis()
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
                        photoUri = photoUriToPersist,
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

            val weekDays = WeekDays(selectedDays)
            selectedTimes.forEach { time ->
                val scheduleId = medicationRepository.insertSchedule(
                    Schedule(id = 0, medicationId = resolvedMedicationId, time = time, daysOfWeek = weekDays)
                )
                alarmScheduler.schedule(scheduleId, resolvedMedicationId, time, weekDays)
            }

            _saveResult.value = MedFormSaveResult.Success
        }
    }

    companion object {
        const val NEW_MEDICATION_ID = -1L
    }
}
