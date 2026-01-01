package com.connect.service.user.dto

data class ResetPasswordRequest(
    val email: String,
    val newPassword: String
)
