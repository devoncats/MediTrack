package com.devoncats.meditrack.domain.repository

import androidx.lifecycle.LiveData
import com.devoncats.meditrack.domain.model.Medication
import com.devoncats.meditrack.domain.model.MedicationLog
import com.devoncats.meditrack.domain.model.MissedDoseAlert
import com.devoncats.meditrack.domain.model.Schedule

interface MedicationRepository {
    suspend fun insertMedication(medication: Medication): Long
    suspend fun updateMedication(medication: Medication)
    suspend fun deleteMedication(medication: Medication)
    suspend fun getMedicationById(id: Long): Medication?
    fun observeMedicationsByOwner(ownerUserId: Long): LiveData<List<Medication>>
    fun observeMedicationById(id: Long): LiveData<Medication?>

    suspend fun insertSchedule(schedule: Schedule): Long
    suspend fun updateSchedule(schedule: Schedule)
    suspend fun deleteSchedule(schedule: Schedule)
    suspend fun getSchedulesByMedication(medicationId: Long): List<Schedule>
    suspend fun getScheduleById(id: Long): Schedule?
    fun observeSchedulesByMedication(medicationId: Long): LiveData<List<Schedule>>

    suspend fun insertLog(log: MedicationLog): Long
    suspend fun updateLog(log: MedicationLog)
    suspend fun deleteLog(log: MedicationLog)
    suspend fun getLogById(id: Long): MedicationLog?
    suspend fun getLatestPendingLogForMedication(medicationId: Long): MedicationLog?
    fun observeLogsByMedication(medicationId: Long): LiveData<List<MedicationLog>>
    fun observeLogsByOwnerBetween(ownerUserId: Long, startInclusive: Long, endExclusive: Long): LiveData<List<MedicationLog>>
    fun observeMissedDoseAlertsForCaregiver(caregiverId: Long): LiveData<List<MissedDoseAlert>>
}
