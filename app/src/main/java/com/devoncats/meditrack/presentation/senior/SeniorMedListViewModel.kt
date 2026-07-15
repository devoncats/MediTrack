package com.devoncats.meditrack.presentation.senior

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.devoncats.meditrack.domain.model.Medication
import com.devoncats.meditrack.domain.model.MedicationLogStatus
import com.devoncats.meditrack.domain.repository.MedicationRepository
import com.devoncats.meditrack.utils.DateUtils
import com.devoncats.meditrack.utils.toHHmm
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

data class SeniorMedicationItem(
    val medication: Medication,
    val todayStatus: MedicationLogStatus?,
    val scheduleSummary: String
)

class SeniorMedListViewModel(
    private val medicationRepository: MedicationRepository,
    seniorUserId: Long
) : ViewModel() {

    private val todayRange = MutableStateFlow(DateUtils.todayRangeMillis())

    // Re-reads the current day boundaries; called from the fragment's onResume so the "today"
    // status doesn't stay pinned to whatever day the ViewModel happened to be created on.
    fun refreshTodayRange() {
        todayRange.value = DateUtils.todayRangeMillis()
    }

    // Flow's combine() suspends the transform lambda itself and cancels any in-flight
    // invocation as soon as a newer emission arrives, so a slow, stale computation can never
    // overwrite the result of a more recent source emission — no manual Job bookkeeping needed.
    @OptIn(ExperimentalCoroutinesApi::class)
    val medicationItems: LiveData<List<SeniorMedicationItem>> = combine(
        medicationRepository.observeMedicationsByOwner(seniorUserId),
        todayRange.flatMapLatest { (startInclusive, endExclusive) ->
            medicationRepository.observeLogsByOwnerBetween(seniorUserId, startInclusive, endExclusive)
        }
    ) { medications, todayLogs ->
        medications.map { medication ->
            val latestLog = todayLogs
                .filter { it.medicationId == medication.id }
                .maxByOrNull { it.scheduledDatetime }
            val schedules = medicationRepository.getSchedulesByMedication(medication.id)
            val scheduleSummary = schedules.map { it.time }.sorted().joinToString(", ") { it.toHHmm() }
            SeniorMedicationItem(medication, latestLog?.status, scheduleSummary)
        }
    }.asLiveData()
}
