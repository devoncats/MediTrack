package com.devoncats.meditrack.domain.model

data class Medication(
    val id: Long,
    val name: String,
    val dose: String,
    val frequency: String,
    val instructions: String?,
    val ownerUserId: Long,
    val photoUri: String?,
    val createdAt: Long
)
