package com.devoncats.meditrack.domain.usecase

import android.util.Patterns
import com.devoncats.meditrack.domain.model.User
import com.devoncats.meditrack.domain.model.UserRole
import com.devoncats.meditrack.domain.repository.UserRepository
import com.devoncats.meditrack.utils.PasswordHasher
import javax.inject.Inject

sealed class RegisterResult {
    data object Success : RegisterResult()
    data object InvalidEmailFormat : RegisterResult()
    data object EmailAlreadyRegistered : RegisterResult()
}

class RegisterUserUseCase @Inject constructor(private val userRepository: UserRepository) {
    suspend operator fun invoke(name: String, email: String, password: String, role: UserRole): RegisterResult {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return RegisterResult.InvalidEmailFormat
        }
        if (userRepository.findByUsername(email) != null) {
            return RegisterResult.EmailAlreadyRegistered
        }
        userRepository.insert(
            User(
                id = 0,
                name = name,
                username = email,
                passwordHash = PasswordHasher.hash(password),
                role = role,
                caregiverId = null
            )
        )
        return RegisterResult.Success
    }
}
