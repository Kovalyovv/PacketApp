package com.example.packetapp.models

import kotlinx.serialization.Serializable


@Serializable
data class JoinGroupRequest(val userId: Int, val inviteCode: String)