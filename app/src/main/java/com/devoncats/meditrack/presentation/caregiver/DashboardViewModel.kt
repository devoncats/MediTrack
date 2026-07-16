package com.devoncats.meditrack.presentation.caregiver

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.devoncats.meditrack.data.local.SessionManager
import com.devoncats.meditrack.domain.model.MissedDoseAlert
import com.devoncats.meditrack.domain.repository.MedicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    medicationRepository: MedicationRepository,
    sessionManager: SessionManager
) : ViewModel() {

    val missedDoseAlerts: LiveData<List<MissedDoseAlert>> =
        medicationRepository.observeMissedDoseAlertsForCaregiver(sessionManager.getUserId()).asLiveData()
}
