package com.devoncats.meditrack.domain.repository

import androidx.lifecycle.LiveData
import com.devoncats.meditrack.domain.model.User

interface UserRepository {
    suspend fun insert(user: User): Long
    suspend fun findByUsername(username: String): User?
    suspend fun findById(id: Long): User?
    suspend fun delete(user: User)
    fun observeSeniorPatientsByCaregiver(caregiverId: Long): LiveData<List<User>>
}
