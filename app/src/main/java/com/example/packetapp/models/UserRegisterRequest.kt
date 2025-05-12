package com.example.packetapp.models

import kotlinx.serialization.Serializable

@Serializable
data class UserRegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String
)
