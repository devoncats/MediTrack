package com.devoncats.meditrack.domain.usecase

import com.devoncats.meditrack.data.local.SessionManager
import com.devoncats.meditrack.domain.model.UserRole
import com.devoncats.meditrack.domain.repository.UserRepository
import com.devoncats.meditrack.utils.PasswordHasher
import javax.inject.Inject

sealed class LoginResult {
    data class Success(val role: UserRole) : LoginResult()
    data object InvalidCredentials : LoginResult()
}

class LoginUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(username: String, password: String): LoginResult {
        val user = userRepository.findByUsername(username)
        if (user == null || !PasswordHasher.verify(password, user.passwordHash)) {
            return LoginResult.InvalidCredentials
        }
        sessionManager.saveSession(user.id, user.role.name)
        return LoginResult.Success(user.role)
    }
}
