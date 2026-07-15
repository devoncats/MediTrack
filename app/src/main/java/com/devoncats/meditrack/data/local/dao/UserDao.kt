package com.devoncats.meditrack.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devoncats.meditrack.data.local.entity.UserEntity

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): UserEntity?

    @Delete
    suspend fun delete(user: UserEntity)

    @Query("SELECT * FROM users WHERE caregiverId = :caregiverId AND role = 'SENIOR_PATIENT' ORDER BY name ASC")
    fun observeByCaregiverId(caregiverId: Long): LiveData<List<UserEntity>>
}
