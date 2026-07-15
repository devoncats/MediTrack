package com.devoncats.meditrack.presentation.caregiver

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.devoncats.meditrack.domain.model.MedicationLogStatus
import com.devoncats.meditrack.domain.model.User
import com.devoncats.meditrack.domain.repository.MedicationRepository
import com.devoncats.meditrack.domain.repository.UserRepository
import com.devoncats.meditrack.services.AlarmScheduler
import com.devoncats.meditrack.services.FileStorageHelper
import com.devoncats.meditrack.utils.DateUtils
import kotlinx.coroutines.launch

data class SeniorListItem(
    val senior: User,
    val todayStatus: MedicationLogStatus?
)

class SeniorListViewModel(
    private val userRepository: UserRepository,
    private val medicationRepository: MedicationRepository,
    private val alarmScheduler: AlarmScheduler,
    private val fileStorageHelper: FileStorageHelper,
    caregiverId: Long
) : ViewModel() {

    private val todayRange = MutableLiveData(DateUtils.todayRangeMillis())

    // Re-reads the current day boundaries; called from the fragment's onResume so the "today"
    // status doesn't stay pinned to whatever day the ViewModel happened to be created on.
    fun refreshTodayRange() {
        val current = DateUtils.todayRangeMillis()
        if (todayRange.value != current) todayRange.value = current
    }

    val seniorItems: LiveData<List<SeniorListItem>> = MediatorLiveData<List<SeniorListItem>>().apply {
        val seniorsLiveData = userRepository.observeSeniorPatientsByCaregiver(caregiverId)
        val statusesLiveData = todayRange.switchMap { (startInclusive, endExclusive) ->
            medicationRepository.observeTodayLogStatusesForCaregiverSeniors(caregiverId, startInclusive, endExclusive)
        }

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
            // Room's CASCADE only cleans up DB rows; alarms/workers live in AlarmManager and
            // WorkManager, and photos live on disk, so they have to be torn down explicitly.
            medicationRepository.getMedicationsByOwner(senior.id).forEach { medication ->
                medicationRepository.getSchedulesByMedication(medication.id).forEach { schedule ->
                    alarmScheduler.cancel(schedule.id)
                }
                medication.photoUri?.takeIf { it.isNotBlank() }?.let { fileStorageHelper.deletePhoto(it) }
            }
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
