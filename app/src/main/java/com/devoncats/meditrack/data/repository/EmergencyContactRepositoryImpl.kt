package com.devoncats.meditrack.data.repository

import com.devoncats.meditrack.data.local.dao.EmergencyContactDao
import com.devoncats.meditrack.data.local.entity.EmergencyContactEntity
import com.devoncats.meditrack.domain.model.EmergencyContact
import com.devoncats.meditrack.domain.repository.EmergencyContactRepository

class EmergencyContactRepositoryImpl(
    private val emergencyContactDao: EmergencyContactDao
) : EmergencyContactRepository {

    override suspend fun insert(contact: EmergencyContact): Long =
        emergencyContactDao.insert(contact.toEntity())

    override suspend fun update(contact: EmergencyContact) =
        emergencyContactDao.update(contact.toEntity())

    override suspend fun findByUserId(userId: Long): EmergencyContact? =
        emergencyContactDao.findByUserId(userId)?.toDomain()

    private fun EmergencyContactEntity.toDomain() = EmergencyContact(
        id = id,
        userId = userId,
        name = name,
        phone = phone
    )

    private fun EmergencyContact.toEntity() = EmergencyContactEntity(
        id = id,
        userId = userId,
        name = name,
        phone = phone
    )
}
