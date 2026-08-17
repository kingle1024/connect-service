package com.connect.service.admin.dto

// 회원의 권한 목록을 전달받은 값으로 통째로 교체한다. (예: ["ROLE_USER", "ROLE_ADMIN"])
data class AdminUserRolesRequest(
    val roles: List<String>
)

// 계정 활성/정지 전환
data class AdminUserEnabledRequest(
    val enabled: Boolean
)

// 관리자가 임시 비밀번호를 지정한다.
data class AdminUserPasswordRequest(
    val newPassword: String
)
