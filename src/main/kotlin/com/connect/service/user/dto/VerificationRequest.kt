package com.connect.service.user.dto

data class VerificationRequest(
    val email: String,
    val code: String
)
