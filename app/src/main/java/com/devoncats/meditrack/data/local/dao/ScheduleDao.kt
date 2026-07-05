package com.devoncats.meditrack.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.devoncats.meditrack.data.local.entity.ScheduleEntity

@Dao
interface ScheduleDao {
    @Insert
    suspend fun insert(schedule: ScheduleEntity): Long

    @Update
    suspend fun update(schedule: ScheduleEntity)

    @Delete
    suspend fun delete(schedule: ScheduleEntity)

    @Query("SELECT * FROM schedules WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): ScheduleEntity?

    @Query("SELECT * FROM schedules WHERE medicationId = :medicationId")
    suspend fun getByMedication(medicationId: Long): List<ScheduleEntity>

    @Query("SELECT * FROM schedules")
    suspend fun getAll(): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE medicationId = :medicationId")
    fun observeByMedication(medicationId: Long): LiveData<List<ScheduleEntity>>
}
