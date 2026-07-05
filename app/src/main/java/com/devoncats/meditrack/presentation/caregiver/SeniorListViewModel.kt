package com.devoncats.meditrack.presentation.caregiver

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devoncats.meditrack.domain.model.MedicationLogStatus
import com.devoncats.meditrack.domain.model.User
import com.devoncats.meditrack.domain.repository.MedicationRepository
import com.devoncats.meditrack.domain.repository.UserRepository
import com.devoncats.meditrack.utils.DateUtils
import kotlinx.coroutines.launch

data class SeniorListItem(
    val senior: User,
    val todayStatus: MedicationLogStatus?
)

class SeniorListViewModel(
    private val userRepository: UserRepository,
    medicationRepository: MedicationRepository,
    caregiverId: Long
) : ViewModel() {

    val seniorItems: LiveData<List<SeniorListItem>> = MediatorLiveData<List<SeniorListItem>>().apply {
        val seniorsLiveData = userRepository.observeSeniorPatientsByCaregiver(caregiverId)
        val (startInclusive, endExclusive) = DateUtils.todayRangeMillis()
        val statusesLiveData = medicationRepository.observeTodayLogStatusesForCaregiverSeniors(
            caregiverId,
            startInclusive,
            endExclusive
        )

        fun combine() {
            val seniors = seniorsLiveData.value ?: return
            val statuses = statusesLiveData.value.orEmpty()
            value = seniors.map { senior ->
                val seniorStatuses = statuses.filter { it.seniorId == senior.id }.map { it.status }
                SeniorListItem(senior, aggregateStatus(seniorStatuses))
            }
        }

        addSource(seniorsLiveData) { combine() }
        addSource(statusesLiveData) { combine() }
    }

    fun deleteSenior(senior: User) {
        viewModelScope.launch {
            userRepository.delete(senior)
        }
    }

    companion object {
        internal fun aggregateStatus(statuses: List<MedicationLogStatus>): MedicationLogStatus? = when {
            MedicationLogStatus.MISSED in statuses -> MedicationLogStatus.MISSED
            MedicationLogStatus.PENDING in statuses -> MedicationLogStatus.PENDING
            statuses.isNotEmpty() -> MedicationLogStatus.CONFIRMED
            else -> null
        }
    }
}
