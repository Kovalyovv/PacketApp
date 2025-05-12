package com.example.packetapp.models

import kotlinx.serialization.Serializable

@Serializable
data class ForgotPasswordRequest(val email: String)