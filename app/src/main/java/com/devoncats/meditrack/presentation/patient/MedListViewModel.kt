package com.devoncats.meditrack.presentation.patient

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.devoncats.meditrack.domain.model.Medication
import com.devoncats.meditrack.domain.model.MedicationLogStatus
import com.devoncats.meditrack.domain.repository.MedicationRepository
import com.devoncats.meditrack.utils.DateUtils
import kotlinx.coroutines.launch

data class MedicationListItem(
    val medication: Medication,
    val todayStatus: MedicationLogStatus?
)

class MedListViewModel(
    private val medicationRepository: MedicationRepository,
    ownerUserId: Long
) : ViewModel() {

    fun findScheduleIdForAlert(medicationId: Long, onResult: (Long?) -> Unit) {
        viewModelScope.launch {
            onResult(medicationRepository.getSchedulesByMedication(medicationId).firstOrNull()?.id)
        }
    }

    private val todayRange = MutableLiveData(DateUtils.todayRangeMillis())

    // Re-reads the current day boundaries; called from the fragment's onResume so the "today"
    // status doesn't stay pinned to whatever day the ViewModel happened to be created on.
    fun refreshTodayRange() {
        val current = DateUtils.todayRangeMillis()
        if (todayRange.value != current) todayRange.value = current
    }

    val medicationItems: LiveData<List<MedicationListItem>> = MediatorLiveData<List<MedicationListItem>>().apply {
        val medicationsLiveData = medicationRepository.observeMedicationsByOwner(ownerUserId)
        val todayLogsLiveData = todayRange.switchMap { (startInclusive, endExclusive) ->
            medicationRepository.observeLogsByOwnerBetween(ownerUserId, startInclusive, endExclusive)
        }

        fun combine() {
            val medications = medicationsLiveData.value ?: return
            val todayLogs = todayLogsLiveData.value.orEmpty()
            value = medications.map { medication ->
                val latestLog = todayLogs
                    .filter { it.medicationId == medication.id }
                    .maxByOrNull { it.scheduledDatetime }
                MedicationListItem(medication, latestLog?.status)
            }
        }

        addSource(medicationsLiveData) { combine() }
        addSource(todayLogsLiveData) { combine() }
    }
}
