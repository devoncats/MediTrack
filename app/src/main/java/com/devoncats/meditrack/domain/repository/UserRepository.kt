package com.devoncats.meditrack.domain.repository

import com.devoncats.meditrack.domain.model.User

interface UserRepository {
    suspend fun insert(user: User): Long
    suspend fun findByEmail(email: String): User?
    suspend fun findById(id: Long): User?
    suspend fun delete(user: User)
}
