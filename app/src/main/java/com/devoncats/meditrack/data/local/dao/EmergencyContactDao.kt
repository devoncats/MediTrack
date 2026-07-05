package com.devoncats.meditrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.devoncats.meditrack.data.local.entity.EmergencyContactEntity

@Dao
interface EmergencyContactDao {
    @Insert
    suspend fun insert(contact: EmergencyContactEntity): Long

    @Update
    suspend fun update(contact: EmergencyContactEntity)

    @Query("SELECT * FROM emergency_contacts WHERE userId = :userId LIMIT 1")
    suspend fun findByUserId(userId: Long): EmergencyContactEntity?
}
