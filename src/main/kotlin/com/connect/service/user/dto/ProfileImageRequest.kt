package com.connect.service.user.dto

// 프로필 이미지 업로드 요청 (base64 이미지 데이터). data URI 접두사(data:image/...;base64,)는 있어도/없어도 처리됨.
data class ProfileImageRequest(
    val imageBase64: String,
    val contentType: String? = null
)
