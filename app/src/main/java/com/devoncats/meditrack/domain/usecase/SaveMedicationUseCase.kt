package com.devoncats.meditrack.domain.usecase

import android.net.Uri
import com.devoncats.meditrack.domain.model.Medication
import com.devoncats.meditrack.domain.model.Schedule
import com.devoncats.meditrack.domain.model.WeekDays
import com.devoncats.meditrack.domain.repository.MedicationRepository
import com.devoncats.meditrack.services.AlarmScheduler
import com.devoncats.meditrack.services.FileStorageHelper
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

sealed class SaveMedicationResult {
    data class Success(val medicationId: Long) : SaveMedicationResult()
    data object ValidationError : SaveMedicationResult()
}

class SaveMedicationUseCase @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val alarmScheduler: AlarmScheduler,
    private val fileStorageHelper: FileStorageHelper
) {
    suspend operator fun invoke(
        ownerUserId: Long,
        existingMedicationId: Long?,
        name: String,
        dose: String,
        frequency: String,
        instructions: String?,
        selectedDays: Set<DayOfWeek>,
        selectedTimes: List<LocalTime>,
        capturedPhotoUri: Uri?
    ): SaveMedicationResult {
        if (name.isBlank() || dose.isBlank() || frequency.isBlank() || selectedDays.isEmpty() || selectedTimes.isEmpty()) {
            return SaveMedicationResult.ValidationError
        }

        val existingMedication = existingMedicationId?.let { medicationRepository.getMedicationById(it) }
        val photoUriToPersist = if (capturedPhotoUri != null) {
            existingMedication?.photoUri?.takeIf { it.isNotBlank() }?.let { fileStorageHelper.deletePhoto(it) }
            fileStorageHelper.savePhoto(capturedPhotoUri)
        } else {
            existingMedication?.photoUri
        }

        val resolvedMedicationId = if (existingMedicationId != null) {
            medicationRepository.updateMedication(
                Medication(
                    id = existingMedicationId,
                    name = name,
                    dose = dose,
                    frequency = frequency,
                    instructions = instructions?.takeIf { it.isNotBlank() },
                    ownerUserId = ownerUserId,
                    photoUri = photoUriToPersist,
                    createdAt = existingMedication?.createdAt ?: System.currentTimeMillis()
                )
            )
            existingMedicationId
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

        if (existingMedicationId != null) {
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

        return SaveMedicationResult.Success(resolvedMedicationId)
    }
}
