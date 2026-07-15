package com.devoncats.meditrack.presentation.caregiver

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devoncats.meditrack.domain.usecase.CreateSeniorPatientResult
import com.devoncats.meditrack.domain.usecase.CreateSeniorPatientUseCase
import kotlinx.coroutines.launch

class CreateSeniorPatientViewModel(
    private val createSeniorPatientUseCase: CreateSeniorPatientUseCase,
    private val caregiverId: Long
) : ViewModel() {

    private val _result = MutableLiveData<CreateSeniorPatientResult>()
    val result: LiveData<CreateSeniorPatientResult> = _result

    fun createSeniorPatient(name: String, contactName: String, contactPhone: String) {
        viewModelScope.launch {
            _result.value = createSeniorPatientUseCase(caregiverId, name, contactName, contactPhone)
        }
    }
}
