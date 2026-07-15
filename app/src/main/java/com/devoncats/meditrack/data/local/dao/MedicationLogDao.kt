package com.devoncats.meditrack.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.devoncats.meditrack.data.local.entity.MedicationLogEntity
import com.devoncats.meditrack.domain.model.MedicationLogStatus

@Dao
interface MedicationLogDao {
    @Insert
    suspend fun insert(log: MedicationLogEntity): Long

    @Update
    suspend fun update(log: MedicationLogEntity)

    @Delete
    suspend fun delete(log: MedicationLogEntity)

    @Query("SELECT * FROM medication_logs WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): MedicationLogEntity?

    @Query(
        "SELECT * FROM medication_logs WHERE scheduleId = :scheduleId AND status = 'PENDING' " +
            "ORDER BY scheduledDatetime DESC LIMIT 1"
    )
    suspend fun findLatestPendingBySchedule(scheduleId: Long): MedicationLogEntity?

    @Query("SELECT * FROM medication_logs WHERE medicationId = :medicationId ORDER BY scheduledDatetime DESC")
    fun observeByMedication(medicationId: Long): LiveData<List<MedicationLogEntity>>

    @Query(
        "SELECT * FROM medication_logs WHERE medicationId = :medicationId " +
            "AND scheduledDatetime >= :startInclusive AND scheduledDatetime < :endExclusive " +
            "ORDER BY scheduledDatetime DESC"
    )
    fun observeByMedicationBetween(
        medicationId: Long,
        startInclusive: Long,
        endExclusive: Long
    ): LiveData<List<MedicationLogEntity>>

    @Query(
        """
        SELECT medication_logs.* FROM medication_logs
        INNER JOIN medications ON medications.id = medication_logs.medicationId
        WHERE medications.ownerUserId = :ownerUserId
          AND medication_logs.scheduledDatetime >= :startInclusive
          AND medication_logs.scheduledDatetime < :endExclusive
        """
    )
    fun observeByOwnerBetween(
        ownerUserId: Long,
        startInclusive: Long,
        endExclusive: Long
    ): LiveData<List<MedicationLogEntity>>

    @Query(
        """
        SELECT medication_logs.id AS logId, users.name AS seniorName, medications.name AS medicationName,
               medication_logs.scheduledDatetime AS scheduledDatetime
        FROM medication_logs
        INNER JOIN medications ON medications.id = medication_logs.medicationId
        INNER JOIN users ON users.id = medications.ownerUserId
        WHERE users.caregiverId = :caregiverId
          AND users.role = 'SENIOR_PATIENT'
          AND medication_logs.status = 'MISSED'
        ORDER BY medication_logs.scheduledDatetime DESC
        """
    )
    fun observeMissedDoseAlertsForCaregiver(caregiverId: Long): LiveData<List<MissedDoseAlertRow>>

    @Query(
        """
        SELECT users.id AS seniorId, medication_logs.status AS status
        FROM medication_logs
        INNER JOIN medications ON medications.id = medication_logs.medicationId
        INNER JOIN users ON users.id = medications.ownerUserId
        WHERE users.caregiverId = :caregiverId
          AND users.role = 'SENIOR_PATIENT'
          AND medication_logs.scheduledDatetime >= :startInclusive
          AND medication_logs.scheduledDatetime < :endExclusive
        """
    )
    fun observeTodayLogStatusesForCaregiverSeniors(
        caregiverId: Long,
        startInclusive: Long,
        endExclusive: Long
    ): LiveData<List<SeniorTodayLogRow>>
}

data class MissedDoseAlertRow(
    val logId: Long,
    val seniorName: String,
    val medicationName: String,
    val scheduledDatetime: Long
)

data class SeniorTodayLogRow(
    val seniorId: Long,
    val status: MedicationLogStatus
)
