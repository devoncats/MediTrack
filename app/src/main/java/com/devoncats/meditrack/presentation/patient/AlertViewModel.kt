package com.devoncats.meditrack.presentation.patient

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devoncats.meditrack.domain.model.MedicationLogStatus
import com.devoncats.meditrack.domain.repository.MedicationRepository
import com.devoncats.meditrack.services.AlarmScheduler
import kotlinx.coroutines.launch

data class AlertInfo(
    val logId: Long,
    val medicationId: Long,
    val medicationName: String,
    val dose: String,
    val scheduledTime: String
)

sealed class AlertActionResult {
    data object Confirmed : AlertActionResult()
    data object Postponed : AlertActionResult()
    data object Dismissed : AlertActionResult()
}

class AlertViewModel(
    private val medicationRepository: MedicationRepository,
    private val alarmScheduler: AlarmScheduler,
    private val scheduleId: Long
) : ViewModel() {

    private val _alertInfo = MutableLiveData<AlertInfo?>()
    val alertInfo: LiveData<AlertInfo?> = _alertInfo

    private val _actionResult = MutableLiveData<AlertActionResult>()
    val actionResult: LiveData<AlertActionResult> = _actionResult

    init {
        viewModelScope.launch {
            val schedule = medicationRepository.getScheduleById(scheduleId) ?: return@launch
            val medication = medicationRepository.getMedicationById(schedule.medicationId) ?: return@launch
            val log = medicationRepository.getLatestPendingLogForSchedule(scheduleId) ?: return@launch

            _alertInfo.value = AlertInfo(
                logId = log.id,
                medicationId = medication.id,
                medicationName = medication.name,
                dose = medication.dose,
                scheduledTime = schedule.time
            )
        }
    }

    fun confirm() {
        val info = _alertInfo.value ?: return
        viewModelScope.launch {
            val log = medicationRepository.getLogById(info.logId) ?: return@launch
            medicationRepository.updateLog(
                log.copy(confirmedAt = System.currentTimeMillis(), status = MedicationLogStatus.CONFIRMED)
            )
            // Line up the next occurrence now that this dose is resolved; this also
            // supersedes the still-pending missed-dose check for this dose.
            medicationRepository.getScheduleById(scheduleId)?.let { schedule ->
                alarmScheduler.schedule(scheduleId, info.medicationId, schedule.time, schedule.daysOfWeek)
            }
            _actionResult.value = AlertActionResult.Confirmed
        }
    }

    fun postpone() {
        val info = _alertInfo.value ?: return
        alarmScheduler.postpone(scheduleId, info.medicationId, info.logId)
        _actionResult.value = AlertActionResult.Postponed
    }

    fun dismiss() {
        _actionResult.value = AlertActionResult.Dismissed
    }
}
