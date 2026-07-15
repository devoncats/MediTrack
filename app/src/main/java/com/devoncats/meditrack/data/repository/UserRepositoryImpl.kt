package com.devoncats.meditrack.data.repository

import com.devoncats.meditrack.data.local.dao.UserDao
import com.devoncats.meditrack.data.local.entity.UserEntity
import com.devoncats.meditrack.domain.model.User
import com.devoncats.meditrack.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRepositoryImpl(private val userDao: UserDao) : UserRepository {

    override suspend fun insert(user: User): Long =
        userDao.insert(user.toEntity())

    override suspend fun findByUsername(username: String): User? =
        userDao.findByUsername(username)?.toDomain()

    override suspend fun findById(id: Long): User? =
        userDao.findById(id)?.toDomain()

    override suspend fun delete(user: User) =
        userDao.delete(user.toEntity())

    override fun observeSeniorPatientsByCaregiver(caregiverId: Long): Flow<List<User>> =
        userDao.observeByCaregiverId(caregiverId).map { list -> list.map { it.toDomain() } }

    private fun UserEntity.toDomain() = User(
        id = id,
        name = name,
        username = username,
        passwordHash = passwordHash,
        role = role,
        caregiverId = caregiverId
    )

    private fun User.toEntity() = UserEntity(
        id = id,
        name = name,
        username = username,
        passwordHash = passwordHash,
        role = role,
        caregiverId = caregiverId
    )
}
