package com.devoncats.meditrack.domain.usecase

import com.devoncats.meditrack.domain.repository.MedicationRepository
import com.devoncats.meditrack.services.AlarmScheduler

class RescheduleAllAlarmsUseCase(
    private val medicationRepository: MedicationRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke() {
        medicationRepository.getAllSchedules().forEach { schedule ->
            alarmScheduler.schedule(schedule.id, schedule.medicationId, schedule.time, schedule.daysOfWeek)
        }
    }
}
