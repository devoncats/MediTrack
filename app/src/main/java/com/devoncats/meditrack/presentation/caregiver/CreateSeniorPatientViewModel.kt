package com.devoncats.meditrack.presentation.caregiver

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devoncats.meditrack.domain.model.EmergencyContact
import com.devoncats.meditrack.domain.model.User
import com.devoncats.meditrack.domain.model.UserRole
import com.devoncats.meditrack.domain.repository.EmergencyContactRepository
import com.devoncats.meditrack.domain.repository.UserRepository
import com.devoncats.meditrack.utils.PasswordHasher
import java.security.SecureRandom
import kotlinx.coroutines.launch

data class GeneratedCredentials(val username: String, val pin: String)

sealed class CreateSeniorPatientResult {
    data class Success(val credentials: GeneratedCredentials) : CreateSeniorPatientResult()
    data object ValidationError : CreateSeniorPatientResult()
}

class CreateSeniorPatientViewModel(
    private val userRepository: UserRepository,
    private val emergencyContactRepository: EmergencyContactRepository,
    private val caregiverId: Long
) : ViewModel() {

    private val _result = MutableLiveData<CreateSeniorPatientResult>()
    val result: LiveData<CreateSeniorPatientResult> = _result

    fun createSeniorPatient(name: String, contactName: String, contactPhone: String) {
        if (name.isBlank()) {
            _result.value = CreateSeniorPatientResult.ValidationError
            return
        }

        viewModelScope.launch {
            val username = uniqueUsername(name)
            val pin = generatePin()

            val seniorId = userRepository.insert(
                User(
                    id = 0,
                    name = name.trim(),
                    email = username,
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
                        name = contactName.trim().ifBlank { name.trim() },
                        phone = contactPhone.trim()
                    )
                )
            }

            _result.value = CreateSeniorPatientResult.Success(GeneratedCredentials(username, pin))
        }
    }

    private suspend fun uniqueUsername(name: String): String {
        val base = buildUsername(name, caregiverId)
        var candidate = base
        var suffix = 1
        while (userRepository.findByEmail(candidate) != null) {
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
