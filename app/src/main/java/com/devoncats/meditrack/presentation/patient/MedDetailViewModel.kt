package com.devoncats.meditrack.presentation.patient

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.devoncats.meditrack.domain.model.Medication
import com.devoncats.meditrack.domain.model.MedicationLog
import com.devoncats.meditrack.domain.repository.MedicationRepository
import com.devoncats.meditrack.domain.usecase.DeleteMedicationUseCase
import com.devoncats.meditrack.presentation.NavArgKeys
import com.devoncats.meditrack.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class MedDetailViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val deleteMedicationUseCase: DeleteMedicationUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val medicationId: Long = savedStateHandle.get<Long>(NavArgKeys.MEDICATION_ID) ?: -1L

    val medication: LiveData<Medication?> = medicationRepository.observeMedicationById(medicationId).asLiveData()

    private val todayRange = MutableStateFlow(DateUtils.todayRangeMillis())

    // Re-reads the current day boundaries; called from the fragment's onResume so "today's
    // history" doesn't stay pinned to whatever day the ViewModel happened to be created on.
    fun refreshTodayRange() {
        todayRange.value = DateUtils.todayRangeMillis()
    }

    // Filtered by query (same criterion as the other list ViewModels) rather than fetching
    // every log for this medication and filtering "today" in memory.
    @OptIn(ExperimentalCoroutinesApi::class)
    val todayLogs: LiveData<List<MedicationLog>> = todayRange.flatMapLatest { (startInclusive, endExclusive) ->
        medicationRepository.observeLogsByMedicationBetween(medicationId, startInclusive, endExclusive)
    }.map { logs -> logs.sortedBy { it.scheduledDatetime } }.asLiveData()

    private val _deleted = MutableLiveData<Boolean>()
    val deleted: LiveData<Boolean> = _deleted

    fun deleteMedication() {
        viewModelScope.launch {
            _deleted.value = deleteMedicationUseCase(medicationId)
        }
    }
}
