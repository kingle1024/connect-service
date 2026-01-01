package com.connect.service.user.dto

import java.time.LocalDateTime

data class VerificationInfo(
    val code: String,
    val expiryTime: LocalDateTime
)
