package com.devoncats.meditrack.presentation.senior

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devoncats.meditrack.domain.repository.MedicationRepository
import com.devoncats.meditrack.domain.usecase.ConfirmDoseUseCase
import com.devoncats.meditrack.presentation.NavArgKeys
import com.devoncats.meditrack.utils.toHHmm
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

data class SeniorAlertInfo(
    val medicationName: String,
    val dose: String,
    val scheduledTime: String
)

@HiltViewModel
class SeniorAlertViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val confirmDoseUseCase: ConfirmDoseUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val scheduleId: Long = savedStateHandle.get<Long>(NavArgKeys.SCHEDULE_ID) ?: -1L

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
            confirmDoseUseCase(currentLogId, scheduleId)
            _closeScreen.value = Unit
        }
    }
}
