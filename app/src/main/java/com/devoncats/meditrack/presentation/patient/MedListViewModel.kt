package com.devoncats.meditrack.presentation.patient

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
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

    val medicationItems: LiveData<List<MedicationListItem>> = MediatorLiveData<List<MedicationListItem>>().apply {
        val medicationsLiveData = medicationRepository.observeMedicationsByOwner(ownerUserId)
        val (startInclusive, endExclusive) = DateUtils.todayRangeMillis()
        val todayLogsLiveData = medicationRepository.observeLogsByOwnerBetween(ownerUserId, startInclusive, endExclusive)

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
