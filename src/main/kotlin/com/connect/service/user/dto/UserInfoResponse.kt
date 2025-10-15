package com.connect.service.user.dto

data class UserInfoResponse(
    val userId: String,
    val email: String,
    val name: String,
    val profileUrl: String? = null // 프로필 URL은 없을 수도 있으므로 nullable
)
