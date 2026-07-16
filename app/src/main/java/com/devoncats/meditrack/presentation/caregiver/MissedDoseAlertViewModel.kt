package com.devoncats.meditrack.presentation.caregiver

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devoncats.meditrack.domain.repository.EmergencyContactRepository
import com.devoncats.meditrack.domain.repository.MedicationRepository
import com.devoncats.meditrack.domain.repository.UserRepository
import com.devoncats.meditrack.presentation.NavArgKeys
import com.devoncats.meditrack.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

data class MissedDoseAlertInfo(
    val seniorName: String,
    val medicationName: String,
    val scheduledTime: String,
    val emergencyContactPhone: String?
)

@HiltViewModel
class MissedDoseAlertViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val userRepository: UserRepository,
    private val emergencyContactRepository: EmergencyContactRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val logId: Long = savedStateHandle.get<Long>(NavArgKeys.LOG_ID) ?: 0L

    private val _alertInfo = MutableLiveData<MissedDoseAlertInfo>()
    val alertInfo: LiveData<MissedDoseAlertInfo> = _alertInfo

    init {
        viewModelScope.launch {
            val log = medicationRepository.getLogById(logId) ?: return@launch
            val medication = medicationRepository.getMedicationById(log.medicationId) ?: return@launch
            val senior = userRepository.findById(medication.ownerUserId) ?: return@launch
            val contact = emergencyContactRepository.findByUserId(senior.id)

            val scheduledTime = DateUtils.formatTime(log.scheduledDatetime)

            _alertInfo.value = MissedDoseAlertInfo(
                seniorName = senior.name,
                medicationName = medication.name,
                scheduledTime = scheduledTime,
                emergencyContactPhone = contact?.phone
            )
        }
    }
}
