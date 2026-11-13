package com.connect.service.user.dto

data class RegistUserRequest(
    val userId: String,
    // 회원가입에서는 이메일을 받지 않는다. 마이페이지의 더존 이메일 인증 때 인증한 주소가 저장된다.
    // (예전 앱이 보내는 값은 형식이 맞으면 그대로 저장한다)
    val email: String? = null,
    val name: String,
    val password: String
)
