package com.devoncats.meditrack.presentation.patient

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devoncats.meditrack.domain.repository.MedicationRepository
import com.devoncats.meditrack.domain.usecase.SaveMedicationResult
import com.devoncats.meditrack.domain.usecase.SaveMedicationUseCase
import java.time.DayOfWeek
import java.time.LocalTime
import kotlinx.coroutines.launch

sealed class MedFormSaveResult {
    data object Success : MedFormSaveResult()
    data object ValidationError : MedFormSaveResult()
}

data class MedFormPrefill(
    val name: String,
    val dose: String,
    val frequency: String,
    val instructions: String?,
    val days: Set<DayOfWeek>,
    val times: List<LocalTime>,
    val photoUri: String?
)

class MedFormViewModel(
    private val medicationRepository: MedicationRepository,
    private val saveMedicationUseCase: SaveMedicationUseCase,
    private val ownerUserId: Long,
    private val medicationId: Long
) : ViewModel() {

    val isEditMode: Boolean = medicationId != NEW_MEDICATION_ID

    private val _prefill = MutableLiveData<MedFormPrefill>()
    val prefill: LiveData<MedFormPrefill> = _prefill

    private val _saveResult = MutableLiveData<MedFormSaveResult>()
    val saveResult: LiveData<MedFormSaveResult> = _saveResult

    init {
        if (isEditMode) {
            viewModelScope.launch {
                val medication = medicationRepository.getMedicationById(medicationId) ?: return@launch
                val schedules = medicationRepository.getSchedulesByMedication(medicationId)
                val days = schedules.firstOrNull()?.daysOfWeek?.days.orEmpty()
                val times = schedules.map { it.time }

                _prefill.value = MedFormPrefill(
                    name = medication.name,
                    dose = medication.dose,
                    frequency = medication.frequency,
                    instructions = medication.instructions,
                    days = days,
                    times = times,
                    photoUri = medication.photoUri
                )
            }
        }
    }

    fun save(
        name: String,
        dose: String,
        frequency: String,
        instructions: String?,
        selectedDays: Set<DayOfWeek>,
        selectedTimes: List<LocalTime>,
        capturedPhotoUri: Uri?
    ) {
        viewModelScope.launch {
            val result = saveMedicationUseCase(
                ownerUserId = ownerUserId,
                existingMedicationId = if (isEditMode) medicationId else null,
                name = name,
                dose = dose,
                frequency = frequency,
                instructions = instructions,
                selectedDays = selectedDays,
                selectedTimes = selectedTimes,
                capturedPhotoUri = capturedPhotoUri
            )
            _saveResult.value = when (result) {
                is SaveMedicationResult.Success -> MedFormSaveResult.Success
                SaveMedicationResult.ValidationError -> MedFormSaveResult.ValidationError
            }
        }
    }

    companion object {
        const val NEW_MEDICATION_ID = -1L
    }
}
