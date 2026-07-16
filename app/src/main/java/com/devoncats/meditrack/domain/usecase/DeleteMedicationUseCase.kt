package com.devoncats.meditrack.domain.usecase

import com.devoncats.meditrack.domain.repository.MedicationRepository
import com.devoncats.meditrack.services.AlarmScheduler
import com.devoncats.meditrack.services.FileStorageHelper
import javax.inject.Inject

class DeleteMedicationUseCase @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val alarmScheduler: AlarmScheduler,
    private val fileStorageHelper: FileStorageHelper
) {
    suspend operator fun invoke(medicationId: Long): Boolean {
        val medication = medicationRepository.getMedicationById(medicationId) ?: return false
        medicationRepository.getSchedulesByMedication(medicationId).forEach { schedule ->
            alarmScheduler.cancel(schedule.id)
        }
        medicationRepository.deleteMedication(medication)
        medication.photoUri?.takeIf { it.isNotBlank() }?.let { fileStorageHelper.deletePhoto(it) }
        return true
    }
}
