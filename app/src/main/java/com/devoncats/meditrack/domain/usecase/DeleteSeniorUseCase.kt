package com.devoncats.meditrack.domain.usecase

import com.devoncats.meditrack.domain.model.User
import com.devoncats.meditrack.domain.repository.MedicationRepository
import com.devoncats.meditrack.domain.repository.UserRepository

class DeleteSeniorUseCase(
    private val userRepository: UserRepository,
    private val medicationRepository: MedicationRepository,
    private val deleteMedicationUseCase: DeleteMedicationUseCase
) {
    suspend operator fun invoke(senior: User) {
        // Room's CASCADE only cleans up DB rows; alarms/workers live in AlarmManager and
        // WorkManager, and photos live on disk, so each medication is torn down explicitly via
        // DeleteMedicationUseCase before the (now medication-less) user row itself is deleted.
        medicationRepository.getMedicationsByOwner(senior.id).forEach { medication ->
            deleteMedicationUseCase(medication.id)
        }
        userRepository.delete(senior)
    }
}
