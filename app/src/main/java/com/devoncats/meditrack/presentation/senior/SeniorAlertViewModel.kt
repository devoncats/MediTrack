package com.devoncats.meditrack.presentation.senior

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devoncats.meditrack.domain.model.MedicationLogStatus
import com.devoncats.meditrack.domain.repository.MedicationRepository
import com.devoncats.meditrack.services.AlarmScheduler
import com.devoncats.meditrack.utils.toHHmm
import kotlinx.coroutines.launch

data class SeniorAlertInfo(
    val medicationName: String,
    val dose: String,
    val scheduledTime: String
)

class SeniorAlertViewModel(
    private val medicationRepository: MedicationRepository,
    private val alarmScheduler: AlarmScheduler,
    private val scheduleId: Long
) : ViewModel() {

    private val _alertInfo = MutableLiveData<SeniorAlertInfo?>()
    val alertInfo: LiveData<SeniorAlertInfo?> = _alertInfo

    // The only outcome for this screen (confirm) is to close it; a sealed result type with a
    // single variant isn't worth it.
    private val _closeScreen = MutableLiveData<Unit>()
    val closeScreen: LiveData<Unit> = _closeScreen

    private var logId: Long? = null

    init {
        viewModelScope.launch {
            val schedule = medicationRepository.getScheduleById(scheduleId) ?: return@launch
            val medication = medicationRepository.getMedicationById(schedule.medicationId) ?: return@launch
            val log = medicationRepository.getLatestPendingLogForSchedule(scheduleId) ?: return@launch

            logId = log.id
            _alertInfo.value = SeniorAlertInfo(
                medicationName = medication.name,
                dose = medication.dose,
                scheduledTime = schedule.time.toHHmm()
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
            // Line up the next occurrence now that this dose is resolved; this also
            // supersedes the still-pending missed-dose check for this dose.
            medicationRepository.getScheduleById(scheduleId)?.let { schedule ->
                alarmScheduler.schedule(scheduleId, log.medicationId, schedule.time, schedule.daysOfWeek)
            }
            _closeScreen.value = Unit
        }
    }
}
