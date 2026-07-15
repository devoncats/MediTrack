package com.devoncats.meditrack.domain.repository

import com.devoncats.meditrack.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun insert(user: User): Long
    suspend fun findByUsername(username: String): User?
    suspend fun findById(id: Long): User?
    suspend fun delete(user: User)
    fun observeSeniorPatientsByCaregiver(caregiverId: Long): Flow<List<User>>
}
