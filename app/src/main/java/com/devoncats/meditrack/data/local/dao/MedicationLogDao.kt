package com.devoncats.meditrack.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.devoncats.meditrack.data.local.entity.MedicationLogEntity

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

    @Query("SELECT * FROM medication_logs WHERE medicationId = :medicationId ORDER BY scheduledDatetime DESC")
    fun observeByMedication(medicationId: Long): LiveData<List<MedicationLogEntity>>

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
}
