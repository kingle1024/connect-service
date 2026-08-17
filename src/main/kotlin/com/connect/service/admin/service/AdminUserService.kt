package com.connect.service.admin.service

import com.connect.service.admin.dto.AdminUserDto
import com.connect.service.admin.dto.PageResponse
import com.connect.service.common.jwt.repository.RefreshTokenRepository
import com.connect.service.user.domain.UserRole
import com.connect.service.user.domain.Users
import com.connect.service.user.repository.UserRepository
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime

/**
 * 백오피스 회원 관리.
 *
 * 별도의 관리자 테이블을 두지 않고 users + ROLE_ADMIN 으로 관리한다.
 * 관리자가 스스로를 잠가버리는 상황을 막기 위한 가드를 각 메서드에 둔다.
 */
@Service
class AdminUserService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @Transactional(readOnly = true)
    fun search(
        keyword: String?,
        role: String?,
        enabled: Boolean?,
        pageable: Pageable
    ): PageResponse<AdminUserDto> {
        val page = userRepository.searchForAdmin(keyword, parseRole(role), enabled, pageable)
        return PageResponse.from(page) { AdminUserDto.from(it) }
    }

    @Transactional(readOnly = true)
    fun get(userId: String): AdminUserDto = AdminUserDto.from(findUser(userId))

    @Transactional
    fun updateRoles(userId: String, roleNames: List<String>, actingAdminUserId: String): AdminUserDto {
        val user = findUser(userId)

        val newRoles = roleNames.mapNotNull { parseRole(it) }.toMutableSet()
        if (newRoles.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "권한을 최소 하나 이상 선택해주세요.")
        }

        val hadAdmin = user.roles.contains(UserRole.ROLE_ADMIN)
        val willHaveAdmin = newRoles.contains(UserRole.ROLE_ADMIN)

        if (hadAdmin && !willHaveAdmin) {
            if (user.userId == actingAdminUserId) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "자신의 관리자 권한은 회수할 수 없습니다.")
            }
            if (userRepository.countByRole(UserRole.ROLE_ADMIN) <= 1) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "마지막 관리자의 권한은 회수할 수 없습니다.")
            }
        }

        user.roles.clear()
        user.roles.addAll(newRoles)
        return AdminUserDto.from(userRepository.save(user))
    }

    @Transactional
    fun setEnabled(userId: String, enabled: Boolean, actingAdminUserId: String): AdminUserDto {
        val user = findUser(userId)

        if (!enabled) {
            if (user.userId == actingAdminUserId) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "자신의 계정은 정지할 수 없습니다.")
            }
            if (user.roles.contains(UserRole.ROLE_ADMIN) &&
                userRepository.countByRole(UserRole.ROLE_ADMIN) <= 1
            ) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "마지막 관리자 계정은 정지할 수 없습니다.")
            }
        }

        user.enabled = enabled
        val saved = userRepository.save(user)

        // 정지된 계정이 기존 Refresh Token 으로 다시 토큰을 받아가지 못하게 한다.
        if (!enabled) {
            revokeRefreshTokens(user)
        }
        return AdminUserDto.from(saved)
    }

    @Transactional
    fun resetPassword(userId: String, newPassword: String): AdminUserDto {
        val user = findUser(userId)

        // 회원가입(AuthController.registerUser)과 동일한 기준을 적용한다.
        if (newPassword.length < 4) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호는 4자리 이상이어야 합니다.")
        }

        user.rawPassword = passwordEncoder.encode(newPassword)
        val saved = userRepository.save(user)

        // 비밀번호가 바뀌었으니 기존 세션(Refresh Token)은 모두 끊는다.
        revokeRefreshTokens(user)
        return AdminUserDto.from(saved)
    }

    private fun revokeRefreshTokens(user: Users) {
        val tokens = refreshTokenRepository.findByUserIdAndIsRevokedFalse(user.userId)
        if (tokens.isEmpty()) {
            return
        }
        val now = LocalDateTime.now()
        tokens.forEach {
            it.isRevoked = true
            it.revokedAt = now
        }
        refreshTokenRepository.saveAll(tokens)
    }

    private fun findUser(userId: String): Users = userRepository.findByUserId(userId)
        ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다: $userId")

    // 화면에서 넘어온 문자열을 UserRole 로 변환. 비어 있으면 null(조건 없음).
    private fun parseRole(role: String?): UserRole? {
        val trimmed = role?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return try {
            UserRole.valueOf(trimmed)
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "알 수 없는 권한입니다: $trimmed")
        }
    }
}
