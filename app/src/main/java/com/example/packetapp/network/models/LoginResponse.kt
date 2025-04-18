package com.example.packetapp.network.models

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val token: String,
    val refreshToken: String,
    val user: UserDTO
)

@Serializable
data class UserDTO(
    val id: Int,
    val name: String,
    val email: String,
    val role: String,
    val createdAt: String
)
