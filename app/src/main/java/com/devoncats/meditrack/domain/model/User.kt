package com.devoncats.meditrack.domain.model

// "username" rather than "email": for SENIOR_PATIENT it holds a synthetic identifier
// (see CreateSeniorPatientUseCase.buildUsername), not a real email address. For PATIENT
// and CAREGIVER it happens to be their email, since that's what the register screen collects.
data class User(
    val id: Long,
    val name: String,
    val username: String,
    val passwordHash: String,
    val role: UserRole,
    val caregiverId: Long?
)
