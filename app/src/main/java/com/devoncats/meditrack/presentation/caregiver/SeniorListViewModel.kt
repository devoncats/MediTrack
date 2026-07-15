package com.devoncats.meditrack.presentation.caregiver

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.devoncats.meditrack.domain.model.MedicationLogStatus
import com.devoncats.meditrack.domain.model.User
import com.devoncats.meditrack.domain.repository.MedicationRepository
import com.devoncats.meditrack.domain.repository.UserRepository
import com.devoncats.meditrack.services.AlarmScheduler
import com.devoncats.meditrack.services.FileStorageHelper
import com.devoncats.meditrack.utils.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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

    private val todayRange = MutableStateFlow(DateUtils.todayRangeMillis())

    // Re-reads the current day boundaries; called from the fragment's onResume so the "today"
    // status doesn't stay pinned to whatever day the ViewModel happened to be created on.
    fun refreshTodayRange() {
        todayRange.value = DateUtils.todayRangeMillis()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val seniorItems: LiveData<List<SeniorListItem>> = combine(
        userRepository.observeSeniorPatientsByCaregiver(caregiverId),
        todayRange.flatMapLatest { (startInclusive, endExclusive) ->
            medicationRepository.observeTodayLogStatusesForCaregiverSeniors(caregiverId, startInclusive, endExclusive)
        }
    ) { seniors, statuses ->
        seniors.map { senior ->
            val seniorStatuses = statuses.filter { it.seniorId == senior.id }.map { it.status }
            SeniorListItem(senior, MedicationLogStatus.aggregate(seniorStatuses))
        }
    }.asLiveData()

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
}
