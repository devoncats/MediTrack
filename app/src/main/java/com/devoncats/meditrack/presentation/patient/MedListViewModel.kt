package com.devoncats.meditrack.presentation.patient

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.devoncats.meditrack.domain.model.Medication
import com.devoncats.meditrack.domain.model.MedicationLogStatus
import com.devoncats.meditrack.domain.repository.MedicationRepository
import com.devoncats.meditrack.utils.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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

    private val todayRange = MutableStateFlow(DateUtils.todayRangeMillis())

    // Re-reads the current day boundaries; called from the fragment's onResume so the "today"
    // status doesn't stay pinned to whatever day the ViewModel happened to be created on.
    fun refreshTodayRange() {
        todayRange.value = DateUtils.todayRangeMillis()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val medicationItems: LiveData<List<MedicationListItem>> = combine(
        medicationRepository.observeMedicationsByOwner(ownerUserId),
        todayRange.flatMapLatest { (startInclusive, endExclusive) ->
            medicationRepository.observeLogsByOwnerBetween(ownerUserId, startInclusive, endExclusive)
        }
    ) { medications, todayLogs ->
        medications.map { medication ->
            val latestLog = todayLogs
                .filter { it.medicationId == medication.id }
                .maxByOrNull { it.scheduledDatetime }
            MedicationListItem(medication, latestLog?.status)
        }
    }.asLiveData()
}
