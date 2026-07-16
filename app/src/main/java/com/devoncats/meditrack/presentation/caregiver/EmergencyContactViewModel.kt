package com.devoncats.meditrack.presentation.caregiver

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devoncats.meditrack.domain.model.EmergencyContact
import com.devoncats.meditrack.domain.repository.EmergencyContactRepository
import com.devoncats.meditrack.presentation.NavArgKeys
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class EmergencyContactViewModel @Inject constructor(
    private val emergencyContactRepository: EmergencyContactRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val seniorUserId: Long = savedStateHandle.get<Long>(NavArgKeys.SENIOR_USER_ID) ?: 0L

    private val _contact = MutableLiveData<EmergencyContact?>()
    val contact: LiveData<EmergencyContact?> = _contact

    init {
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            _contact.value = emergencyContactRepository.findByUserId(seniorUserId)
        }
    }

    fun saveContact(name: String, phone: String) {
        viewModelScope.launch {
            val existing = emergencyContactRepository.findByUserId(seniorUserId)
            if (existing != null) {
                emergencyContactRepository.update(existing.copy(name = name, phone = phone))
            } else {
                emergencyContactRepository.insert(
                    EmergencyContact(id = 0, userId = seniorUserId, name = name, phone = phone)
                )
            }
            refresh()
        }
    }
}
