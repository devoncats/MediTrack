package com.devoncats.meditrack.domain.usecase

import com.devoncats.meditrack.domain.model.EmergencyContact
import com.devoncats.meditrack.domain.model.User
import com.devoncats.meditrack.domain.model.UserRole
import com.devoncats.meditrack.domain.repository.EmergencyContactRepository
import com.devoncats.meditrack.domain.repository.UserRepository
import com.devoncats.meditrack.utils.PasswordHasher
import java.security.SecureRandom

data class GeneratedCredentials(val username: String, val pin: String)

sealed class CreateSeniorPatientResult {
    data class Success(val credentials: GeneratedCredentials) : CreateSeniorPatientResult()
    data object ValidationError : CreateSeniorPatientResult()
}

class CreateSeniorPatientUseCase(
    private val userRepository: UserRepository,
    private val emergencyContactRepository: EmergencyContactRepository
) {
    suspend operator fun invoke(
        caregiverId: Long,
        name: String,
        contactName: String,
        contactPhone: String
    ): CreateSeniorPatientResult {
        if (name.isBlank()) {
            return CreateSeniorPatientResult.ValidationError
        }

        val trimmedName = name.trim()
        val username = uniqueUsername(trimmedName, caregiverId)
        val pin = generatePin()

        val seniorId = userRepository.insert(
            User(
                id = 0,
                name = trimmedName,
                username = username,
                passwordHash = PasswordHasher.hash(pin),
                role = UserRole.SENIOR_PATIENT,
                caregiverId = caregiverId
            )
        )

        if (contactPhone.isNotBlank()) {
            emergencyContactRepository.insert(
                EmergencyContact(
                    id = 0,
                    userId = seniorId,
                    name = contactName.trim().ifBlank { trimmedName },
                    phone = contactPhone.trim()
                )
            )
        }

        return CreateSeniorPatientResult.Success(GeneratedCredentials(username, pin))
    }

    private suspend fun uniqueUsername(name: String, caregiverId: Long): String {
        val base = buildUsername(name, caregiverId)
        var candidate = base
        var suffix = 1
        while (userRepository.findByUsername(candidate) != null) {
            candidate = "${base}_$suffix"
            suffix++
        }
        return candidate
    }

    companion object {
        internal fun buildUsername(name: String, caregiverId: Long): String {
            val normalized = name.trim().lowercase()
                .replace(Regex("[^a-z0-9]+"), "_")
                .trim('_')
            return "pm_${normalized}_$caregiverId"
        }

        // The PIN is a login credential (like a password), so it needs a
        // cryptographically secure source, not kotlin.random.Random's predictable PRNG.
        internal fun generatePin(): String = "%06d".format(SecureRandom().nextInt(1_000_000))
    }
}
