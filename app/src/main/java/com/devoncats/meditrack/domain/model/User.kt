package com.devoncats.meditrack.domain.model

data class User(
    val id: Long,
    val name: String,
    val email: String,
    val passwordHash: String,
    val role: UserRole,
    val caregiverId: Long?
)
