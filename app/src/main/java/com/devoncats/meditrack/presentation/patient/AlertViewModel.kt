package com.devoncats.meditrack.presentation.patient

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devoncats.meditrack.domain.repository.MedicationRepository
import com.devoncats.meditrack.domain.usecase.ConfirmDoseUseCase
import com.devoncats.meditrack.domain.usecase.PostponeDoseUseCase
import com.devoncats.meditrack.utils.toHHmm
import kotlinx.coroutines.launch

data class AlertInfo(
    val logId: Long,
    val medicationId: Long,
    val medicationName: String,
    val dose: String,
    val scheduledTime: String
)

class AlertViewModel(
    private val medicationRepository: MedicationRepository,
    private val confirmDoseUseCase: ConfirmDoseUseCase,
    private val postponeDoseUseCase: PostponeDoseUseCase,
    private val scheduleId: Long
) : ViewModel() {

    private val _alertInfo = MutableLiveData<AlertInfo?>()
    val alertInfo: LiveData<AlertInfo?> = _alertInfo

    // Confirm/postpone/dismiss all resolve to the same outcome for this screen: close it.
    // A richer result type isn't worth it while nothing downstream distinguishes between them.
    private val _closeScreen = MutableLiveData<Unit>()
    val closeScreen: LiveData<Unit> = _closeScreen

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
                scheduledTime = schedule.time.toHHmm()
            )
        }
    }

    fun confirm() {
        val info = _alertInfo.value ?: return
        viewModelScope.launch {
            confirmDoseUseCase(info.logId, scheduleId)
            _closeScreen.value = Unit
        }
    }

    fun postpone() {
        val info = _alertInfo.value ?: return
        postponeDoseUseCase(scheduleId, info.medicationId, info.logId)
        _closeScreen.value = Unit
    }

    fun dismiss() {
        _closeScreen.value = Unit
    }
}
