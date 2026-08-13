package com.connect.service.admin.dto

import com.connect.service.user.domain.UserRole
import com.connect.service.user.domain.Users

data class AdminUserDto(
    val id: Long,
    val userId: String,
    val email: String,
    val name: String,
    val profileUrl: String?,
    val roles: List<String>,
    val enabled: Boolean,
    val admin: Boolean,
    val verified: Boolean
) {
    companion object {
        fun from(user: Users): AdminUserDto = AdminUserDto(
            id = user.id ?: throw IllegalStateException("회원 ID가 없습니다."),
            userId = user.userId,
            email = user.email,
            name = user.name,
            profileUrl = user.profileUrl,
            roles = user.roles.map { it.name }.sorted(),
            enabled = user.enabled,
            admin = user.roles.contains(UserRole.ROLE_ADMIN),
            verified = user.roles.contains(UserRole.ROLE_VERIFIED)
        )
    }
}
