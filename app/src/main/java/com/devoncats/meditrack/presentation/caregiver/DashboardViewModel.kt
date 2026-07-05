package com.devoncats.meditrack.presentation.caregiver

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.devoncats.meditrack.domain.model.MissedDoseAlert
import com.devoncats.meditrack.domain.repository.MedicationRepository

class DashboardViewModel(
    medicationRepository: MedicationRepository,
    caregiverId: Long
) : ViewModel() {

    val missedDoseAlerts: LiveData<List<MissedDoseAlert>> =
        medicationRepository.observeMissedDoseAlertsForCaregiver(caregiverId)
}
