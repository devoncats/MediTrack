package com.devoncats.meditrack.presentation.caregiver

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devoncats.meditrack.domain.repository.EmergencyContactRepository
import com.devoncats.meditrack.domain.repository.MedicationRepository
import com.devoncats.meditrack.domain.repository.UserRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

data class MissedDoseAlertInfo(
    val seniorName: String,
    val medicationName: String,
    val scheduledTime: String,
    val emergencyContactPhone: String?
)

class MissedDoseAlertViewModel(
    private val medicationRepository: MedicationRepository,
    private val userRepository: UserRepository,
    private val emergencyContactRepository: EmergencyContactRepository,
    private val logId: Long
) : ViewModel() {

    private val _alertInfo = MutableLiveData<MissedDoseAlertInfo>()
    val alertInfo: LiveData<MissedDoseAlertInfo> = _alertInfo

    init {
        viewModelScope.launch {
            val log = medicationRepository.getLogById(logId) ?: return@launch
            val medication = medicationRepository.getMedicationById(log.medicationId) ?: return@launch
            val senior = userRepository.findById(medication.ownerUserId) ?: return@launch
            val contact = emergencyContactRepository.findByUserId(senior.id)

            val scheduledTime = Instant.ofEpochMilli(log.scheduledDatetime)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
                .format(DateTimeFormatter.ofPattern("HH:mm"))

            _alertInfo.value = MissedDoseAlertInfo(
                seniorName = senior.name,
                medicationName = medication.name,
                scheduledTime = scheduledTime,
                emergencyContactPhone = contact?.phone
            )
        }
    }
}
