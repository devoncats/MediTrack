package com.devoncats.meditrack.presentation.senior

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devoncats.meditrack.domain.model.MedicationLogStatus
import com.devoncats.meditrack.domain.repository.MedicationRepository
import com.devoncats.meditrack.services.AlarmScheduler
import kotlinx.coroutines.launch

data class SeniorAlertInfo(
    val medicationName: String,
    val dose: String,
    val scheduledTime: String
)

sealed class SeniorAlertActionResult {
    data object Confirmed : SeniorAlertActionResult()
}

class SeniorAlertViewModel(
    private val medicationRepository: MedicationRepository,
    private val alarmScheduler: AlarmScheduler,
    private val scheduleId: Long
) : ViewModel() {

    private val _alertInfo = MutableLiveData<SeniorAlertInfo?>()
    val alertInfo: LiveData<SeniorAlertInfo?> = _alertInfo

    private val _actionResult = MutableLiveData<SeniorAlertActionResult>()
    val actionResult: LiveData<SeniorAlertActionResult> = _actionResult

    private var logId: Long? = null

    init {
        viewModelScope.launch {
            val schedule = medicationRepository.getScheduleById(scheduleId) ?: return@launch
            val medication = medicationRepository.getMedicationById(schedule.medicationId) ?: return@launch
            val log = medicationRepository.getLatestPendingLogForMedication(medication.id) ?: return@launch

            logId = log.id
            _alertInfo.value = SeniorAlertInfo(
                medicationName = medication.name,
                dose = medication.dose,
                scheduledTime = schedule.time
            )
        }
    }

    fun confirm() {
        val currentLogId = logId ?: return
        viewModelScope.launch {
            val log = medicationRepository.getLogById(currentLogId) ?: return@launch
            medicationRepository.updateLog(
                log.copy(confirmedAt = System.currentTimeMillis(), status = MedicationLogStatus.CONFIRMED)
            )
            alarmScheduler.cancelMissedDoseCheck(scheduleId)
            _actionResult.value = SeniorAlertActionResult.Confirmed
        }
    }
}
