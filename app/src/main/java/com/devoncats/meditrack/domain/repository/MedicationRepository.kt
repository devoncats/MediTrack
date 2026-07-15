package com.devoncats.meditrack.domain.repository

import com.devoncats.meditrack.domain.model.Medication
import com.devoncats.meditrack.domain.model.MedicationLog
import com.devoncats.meditrack.domain.model.MissedDoseAlert
import com.devoncats.meditrack.domain.model.Schedule
import com.devoncats.meditrack.domain.model.SeniorDoseStatus
import kotlinx.coroutines.flow.Flow

interface MedicationRepository {
    suspend fun insertMedication(medication: Medication): Long
    suspend fun updateMedication(medication: Medication)
    suspend fun deleteMedication(medication: Medication)
    suspend fun getMedicationById(id: Long): Medication?
    suspend fun getMedicationsByOwner(ownerUserId: Long): List<Medication>
    fun observeMedicationsByOwner(ownerUserId: Long): Flow<List<Medication>>
    fun observeMedicationById(id: Long): Flow<Medication?>

    suspend fun insertSchedule(schedule: Schedule): Long
    suspend fun updateSchedule(schedule: Schedule)
    suspend fun deleteSchedule(schedule: Schedule)
    suspend fun getSchedulesByMedication(medicationId: Long): List<Schedule>
    suspend fun getScheduleById(id: Long): Schedule?
    fun observeSchedulesByMedication(medicationId: Long): Flow<List<Schedule>>

    suspend fun insertLog(log: MedicationLog): Long
    suspend fun updateLog(log: MedicationLog)
    suspend fun deleteLog(log: MedicationLog)
    suspend fun getLogById(id: Long): MedicationLog?
    suspend fun getLatestPendingLogForSchedule(scheduleId: Long): MedicationLog?
    fun observeLogsByMedication(medicationId: Long): Flow<List<MedicationLog>>
    fun observeLogsByMedicationBetween(
        medicationId: Long,
        startInclusive: Long,
        endExclusive: Long
    ): Flow<List<MedicationLog>>
    fun observeLogsByOwnerBetween(ownerUserId: Long, startInclusive: Long, endExclusive: Long): Flow<List<MedicationLog>>
    fun observeMissedDoseAlertsForCaregiver(caregiverId: Long): Flow<List<MissedDoseAlert>>
    fun observeTodayLogStatusesForCaregiverSeniors(
        caregiverId: Long,
        startInclusive: Long,
        endExclusive: Long
    ): Flow<List<SeniorDoseStatus>>
}
