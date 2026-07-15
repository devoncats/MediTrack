package com.devoncats.meditrack

import androidx.test.platform.app.InstrumentationRegistry
import com.devoncats.meditrack.data.local.MediTrackDatabase
import com.devoncats.meditrack.data.local.SessionManager
import com.devoncats.meditrack.data.local.entity.UserEntity
import com.devoncats.meditrack.domain.model.UserRole
import com.devoncats.meditrack.utils.PasswordHasher

suspend fun seedCaregiverAndLogIn(
    email: String,
    name: String = "Caregiver Test",
    password: String = "CaregiverPass123!"
): Long {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val userDao = MediTrackDatabase.getInstance(context).userDao()

    userDao.findByUsername(email)?.let { userDao.delete(it) }
    val caregiverId = userDao.insert(
        UserEntity(
            name = name,
            username = email,
            passwordHash = PasswordHasher.hash(password),
            role = UserRole.CAREGIVER,
            caregiverId = null
        )
    )
    SessionManager(context).saveSession(caregiverId, UserRole.CAREGIVER.name)
    return caregiverId
}

suspend fun clearSessionAndDeleteUsers(vararg emails: String) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    SessionManager(context).clearSession()
    val userDao = MediTrackDatabase.getInstance(context).userDao()
    emails.forEach { email -> userDao.findByUsername(email)?.let { userDao.delete(it) } }
}
