package com.devoncats.meditrack.presentation.caregiver

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devoncats.meditrack.domain.model.EmergencyContact
import com.devoncats.meditrack.domain.repository.EmergencyContactRepository
import kotlinx.coroutines.launch

class EmergencyContactViewModel(
    private val emergencyContactRepository: EmergencyContactRepository,
    private val seniorUserId: Long
) : ViewModel() {

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
