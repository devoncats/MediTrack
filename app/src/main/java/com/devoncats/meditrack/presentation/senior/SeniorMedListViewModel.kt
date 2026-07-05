package com.devoncats.meditrack.presentation.senior

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devoncats.meditrack.domain.model.Medication
import com.devoncats.meditrack.domain.model.MedicationLogStatus
import com.devoncats.meditrack.domain.repository.MedicationRepository
import com.devoncats.meditrack.utils.DateUtils
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

    init {
        val medicationsLiveData = medicationRepository.observeMedicationsByOwner(seniorUserId)
        val (startInclusive, endExclusive) = DateUtils.todayRangeMillis()
        val todayLogsLiveData = medicationRepository.observeLogsByOwnerBetween(seniorUserId, startInclusive, endExclusive)

        fun combine() {
            val medications = medicationsLiveData.value ?: return
            val todayLogs = todayLogsLiveData.value.orEmpty()
            viewModelScope.launch {
                _medicationItems.value = medications.map { medication ->
                    val latestLog = todayLogs
                        .filter { it.medicationId == medication.id }
                        .maxByOrNull { it.scheduledDatetime }
                    val schedules = medicationRepository.getSchedulesByMedication(medication.id)
                    val scheduleSummary = schedules.map { it.time }.sorted().joinToString(", ")
                    SeniorMedicationItem(medication, latestLog?.status, scheduleSummary)
                }
            }
        }

        _medicationItems.addSource(medicationsLiveData) { combine() }
        _medicationItems.addSource(todayLogsLiveData) { combine() }
    }
}
