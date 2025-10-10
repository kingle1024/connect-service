package com.connect.service.user.dto

data class AuthResponse(
    val accessToken: String, // Access Token
    val refreshToken: String, // Refresh Token
    val tokenType: String = "Bearer"
)
