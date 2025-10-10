package com.connect.service.user.dto

data class RegistUserRequest(
    val userId: String,
    val email: String,
    val name: String,
    val password: String
)
