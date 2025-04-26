package com.example.packetapp.network.models

import kotlinx.serialization.Serializable


@Serializable
data class JoinGroupRequest(val userId: Int, val inviteCode: String)