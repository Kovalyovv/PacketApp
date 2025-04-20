package com.example.packetapp.network.models

import kotlinx.serialization.Serializable

@Serializable
data class ForgotPasswordRequest(val email: String)