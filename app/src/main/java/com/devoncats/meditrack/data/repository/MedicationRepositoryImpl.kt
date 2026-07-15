package com.devoncats.meditrack.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.devoncats.meditrack.data.local.dao.MedicationDao
import com.devoncats.meditrack.data.local.dao.MedicationLogDao
import com.devoncats.meditrack.data.local.dao.ScheduleDao
import com.devoncats.meditrack.data.local.entity.MedicationEntity
import com.devoncats.meditrack.data.local.entity.MedicationLogEntity
import com.devoncats.meditrack.data.local.entity.ScheduleEntity
import com.devoncats.meditrack.domain.model.Medication
import com.devoncats.meditrack.domain.model.MedicationLog
import com.devoncats.meditrack.domain.model.MissedDoseAlert
import com.devoncats.meditrack.domain.model.Schedule
import com.devoncats.meditrack.domain.model.SeniorDoseStatus
import com.devoncats.meditrack.domain.repository.MedicationRepository

class MedicationRepositoryImpl(
    private val medicationDao: MedicationDao,
    private val scheduleDao: ScheduleDao,
    private val medicationLogDao: MedicationLogDao
) : MedicationRepository {

    override suspend fun insertMedication(medication: Medication): Long =
        medicationDao.insert(medication.toEntity())

    override suspend fun updateMedication(medication: Medication) =
        medicationDao.update(medication.toEntity())

    override suspend fun deleteMedication(medication: Medication) =
        medicationDao.delete(medication.toEntity())

    override suspend fun getMedicationById(id: Long): Medication? =
        medicationDao.findById(id)?.toDomain()

    override suspend fun getMedicationsByOwner(ownerUserId: Long): List<Medication> =
        medicationDao.getByOwner(ownerUserId).map { it.toDomain() }

    override fun observeMedicationsByOwner(ownerUserId: Long): LiveData<List<Medication>> =
        medicationDao.observeByOwner(ownerUserId).map { list -> list.map { it.toDomain() } }

    override fun observeMedicationById(id: Long): LiveData<Medication?> =
        medicationDao.observeById(id).map { it?.toDomain() }

    override suspend fun insertSchedule(schedule: Schedule): Long =
        scheduleDao.insert(schedule.toEntity())

    override suspend fun updateSchedule(schedule: Schedule) =
        scheduleDao.update(schedule.toEntity())

    override suspend fun deleteSchedule(schedule: Schedule) =
        scheduleDao.delete(schedule.toEntity())

    override suspend fun getSchedulesByMedication(medicationId: Long): List<Schedule> =
        scheduleDao.getByMedication(medicationId).map { it.toDomain() }

    override suspend fun getScheduleById(id: Long): Schedule? =
        scheduleDao.findById(id)?.toDomain()

    override fun observeSchedulesByMedication(medicationId: Long): LiveData<List<Schedule>> =
        scheduleDao.observeByMedication(medicationId).map { list -> list.map { it.toDomain() } }

    override suspend fun insertLog(log: MedicationLog): Long =
        medicationLogDao.insert(log.toEntity())

    override suspend fun updateLog(log: MedicationLog) =
        medicationLogDao.update(log.toEntity())

    override suspend fun deleteLog(log: MedicationLog) =
        medicationLogDao.delete(log.toEntity())

    override suspend fun getLogById(id: Long): MedicationLog? =
        medicationLogDao.findById(id)?.toDomain()

    override suspend fun getLatestPendingLogForSchedule(scheduleId: Long): MedicationLog? =
        medicationLogDao.findLatestPendingBySchedule(scheduleId)?.toDomain()

    override fun observeLogsByMedication(medicationId: Long): LiveData<List<MedicationLog>> =
        medicationLogDao.observeByMedication(medicationId).map { list -> list.map { it.toDomain() } }

    override fun observeLogsByMedicationBetween(
        medicationId: Long,
        startInclusive: Long,
        endExclusive: Long
    ): LiveData<List<MedicationLog>> =
        medicationLogDao.observeByMedicationBetween(medicationId, startInclusive, endExclusive)
            .map { list -> list.map { it.toDomain() } }

    override fun observeLogsByOwnerBetween(
        ownerUserId: Long,
        startInclusive: Long,
        endExclusive: Long
    ): LiveData<List<MedicationLog>> =
        medicationLogDao.observeByOwnerBetween(ownerUserId, startInclusive, endExclusive)
            .map { list -> list.map { it.toDomain() } }

    override fun observeMissedDoseAlertsForCaregiver(caregiverId: Long): LiveData<List<MissedDoseAlert>> =
        medicationLogDao.observeMissedDoseAlertsForCaregiver(caregiverId).map { rows ->
            rows.map {
                MissedDoseAlert(
                    logId = it.logId,
                    seniorName = it.seniorName,
                    medicationName = it.medicationName,
                    scheduledDatetime = it.scheduledDatetime
                )
            }
        }

    override fun observeTodayLogStatusesForCaregiverSeniors(
        caregiverId: Long,
        startInclusive: Long,
        endExclusive: Long
    ): LiveData<List<SeniorDoseStatus>> =
        medicationLogDao.observeTodayLogStatusesForCaregiverSeniors(caregiverId, startInclusive, endExclusive)
            .map { rows -> rows.map { SeniorDoseStatus(it.seniorId, it.status) } }

    private fun MedicationEntity.toDomain() = Medication(
        id = id,
        name = name,
        dose = dose,
        frequency = frequency,
        instructions = instructions,
        ownerUserId = ownerUserId,
        photoUri = photoUri,
        createdAt = createdAt
    )

    private fun Medication.toEntity() = MedicationEntity(
        id = id,
        name = name,
        dose = dose,
        frequency = frequency,
        instructions = instructions,
        ownerUserId = ownerUserId,
        photoUri = photoUri,
        createdAt = createdAt
    )

    private fun ScheduleEntity.toDomain() = Schedule(
        id = id,
        medicationId = medicationId,
        time = time,
        daysOfWeek = daysOfWeek
    )

    private fun Schedule.toEntity() = ScheduleEntity(
        id = id,
        medicationId = medicationId,
        time = time,
        daysOfWeek = daysOfWeek
    )

    private fun MedicationLogEntity.toDomain() = MedicationLog(
        id = id,
        medicationId = medicationId,
        scheduleId = scheduleId,
        scheduledDatetime = scheduledDatetime,
        confirmedAt = confirmedAt,
        status = status
    )

    private fun MedicationLog.toEntity() = MedicationLogEntity(
        id = id,
        medicationId = medicationId,
        scheduleId = scheduleId,
        scheduledDatetime = scheduledDatetime,
        confirmedAt = confirmedAt,
        status = status
    )
}
