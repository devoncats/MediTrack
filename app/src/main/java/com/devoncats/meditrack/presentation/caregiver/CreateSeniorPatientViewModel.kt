package com.devoncats.meditrack.presentation.caregiver

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devoncats.meditrack.data.local.SessionManager
import com.devoncats.meditrack.domain.usecase.CreateSeniorPatientResult
import com.devoncats.meditrack.domain.usecase.CreateSeniorPatientUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class CreateSeniorPatientViewModel @Inject constructor(
    private val createSeniorPatientUseCase: CreateSeniorPatientUseCase,
    sessionManager: SessionManager
) : ViewModel() {

    private val caregiverId: Long = sessionManager.getUserId()

    private val _result = MutableLiveData<CreateSeniorPatientResult>()
    val result: LiveData<CreateSeniorPatientResult> = _result

    fun createSeniorPatient(name: String, contactName: String, contactPhone: String) {
        viewModelScope.launch {
            _result.value = createSeniorPatientUseCase(caregiverId, name, contactName, contactPhone)
        }
    }
}
