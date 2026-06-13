package com.connect.service.user.domain

enum class UserRole {
    ROLE_USER,
    ROLE_ADMIN,
    ROLE_VERIFIED // 더존 이메일 인증 완료 사용자 (인증 마크 표시)
}
