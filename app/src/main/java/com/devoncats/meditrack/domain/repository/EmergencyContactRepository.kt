package com.devoncats.meditrack.domain.repository

import com.devoncats.meditrack.domain.model.EmergencyContact

interface EmergencyContactRepository {
    suspend fun insert(contact: EmergencyContact): Long
    suspend fun update(contact: EmergencyContact)
    suspend fun findByUserId(userId: Long): EmergencyContact?
}
