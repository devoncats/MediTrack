package com.devoncats.meditrack.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.devoncats.meditrack.data.local.entity.MedicationEntity

@Dao
interface MedicationDao {
    @Insert
    suspend fun insert(medication: MedicationEntity): Long

    @Update
    suspend fun update(medication: MedicationEntity)

    @Delete
    suspend fun delete(medication: MedicationEntity)

    @Query("SELECT * FROM medications WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): MedicationEntity?

    @Query("SELECT * FROM medications WHERE ownerUserId = :ownerUserId ORDER BY name ASC")
    fun observeByOwner(ownerUserId: Long): LiveData<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE id = :id LIMIT 1")
    fun observeById(id: Long): LiveData<MedicationEntity?>
}
