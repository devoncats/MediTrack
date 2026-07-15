package com.devoncats.meditrack.presentation.senior

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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class SeniorMedicationItem(
    val medication: Medication,
    val todayStatus: MedicationLogStatus?,
    val scheduleSummary: String
)

class SeniorMedListViewModel(
    private val medicationRepository: MedicationRepository,
    seniorUserId: Long
) : ViewModel() {

    private val _medicationItems = MediatorLiveData<List<SeniorMedicationItem>>()
    val medicationItems: LiveData<List<SeniorMedicationItem>> = _medicationItems

    private val todayRange = MutableLiveData(DateUtils.todayRangeMillis())

    // Re-reads the current day boundaries; called from the fragment's onResume so the "today"
    // status doesn't stay pinned to whatever day the ViewModel happened to be created on.
    fun refreshTodayRange() {
        val current = DateUtils.todayRangeMillis()
        if (todayRange.value != current) todayRange.value = current
    }

    init {
        val medicationsLiveData = medicationRepository.observeMedicationsByOwner(seniorUserId)
        val todayLogsLiveData = todayRange.switchMap { (startInclusive, endExclusive) ->
            medicationRepository.observeLogsByOwnerBetween(seniorUserId, startInclusive, endExclusive)
        }

        // Cancels the in-flight combine, if any, before starting a new one so a slow, stale
        // computation can never overwrite the result of a more recent source emission.
        var combineJob: Job? = null

        fun combine() {
            val medications = medicationsLiveData.value ?: return
            val todayLogs = todayLogsLiveData.value.orEmpty()
            combineJob?.cancel()
            combineJob = viewModelScope.launch {
                val items = medications.map { medication ->
                    val latestLog = todayLogs
                        .filter { it.medicationId == medication.id }
                        .maxByOrNull { it.scheduledDatetime }
                    val schedules = medicationRepository.getSchedulesByMedication(medication.id)
                    val scheduleSummary = schedules.map { it.time }.sorted().joinToString(", ")
                    SeniorMedicationItem(medication, latestLog?.status, scheduleSummary)
                }
                _medicationItems.value = items
            }
        }

        _medicationItems.addSource(medicationsLiveData) { combine() }
        _medicationItems.addSource(todayLogsLiveData) { combine() }
    }
}
