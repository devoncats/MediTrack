package com.devoncats.meditrack.domain.model

data class EmergencyContact(
    val id: Long,
    val userId: Long,
    val name: String,
    val phone: String
)
