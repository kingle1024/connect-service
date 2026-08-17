package com.connect.service.admin.controller

import com.connect.service.admin.dto.AdminUserDto
import com.connect.service.common.ApiResponse
import com.connect.service.common.jwt.JwtTokenProvider
import com.connect.service.common.jwt.dto.RefreshToken
import com.connect.service.common.jwt.repository.RefreshTokenRepository
import com.connect.service.user.domain.UserRole
import com.connect.service.user.dto.AuthResponse
import com.connect.service.user.dto.LoginRequest
import com.connect.service.user.repository.UserRepository
import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 백오피스 로그인.
 *
 * 계정은 기존 users 테이블을 그대로 쓰고, ROLE_ADMIN 을 가진 계정만 통과시킨다.
 * 발급되는 토큰은 일반 로그인과 동일한 JWT 다.
 */
@RestController
@RequestMapping("/api/admin/auth")
class AdminAuthController(
    private val authenticationManager: AuthenticationManager,
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository
) {
    companion object {
        private val log = KotlinLogging.logger {}
    }

    @PostMapping("/login")
    fun login(
        @RequestBody loginRequest: LoginRequest,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<AuthResponse>> {
        val authentication: Authentication = try {
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(loginRequest.userId, loginRequest.password)
            )
        } catch (ex: Exception) {
            log.warn("백오피스 로그인 실패: 사용자 ID = {}, 이유 = {}", loginRequest.userId, ex.message)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("아이디 또는 비밀번호가 올바르지 않습니다."))
        }

        // 인증은 됐지만 관리자가 아니면 여기서 막는다. (일반 회원 계정으로는 백오피스에 들어올 수 없다)
        val isAdmin = authentication.authorities.any { it.authority == UserRole.ROLE_ADMIN.name }
        if (!isAdmin) {
            log.warn("백오피스 접근 거부: 관리자 권한 없음. 사용자 ID = {}", loginRequest.userId)
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("관리자 권한이 없는 계정입니다."))
        }

        val accessToken = jwtTokenProvider.createAccessToken(authentication)
        val (refreshTokenString, refreshTokenExpiresAt) = jwtTokenProvider.createRefreshToken(authentication)

        refreshTokenRepository.save(
            RefreshToken(
                userId = authentication.name,
                token = refreshTokenString,
                issuedAt = LocalDateTime.now(),
                expiresAt = refreshTokenExpiresAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
                ipAddress = request.remoteAddr,
                userAgent = request.getHeader("User-Agent")
            )
        )

        log.info("백오피스 로그인 성공: 사용자 ID = {}", authentication.name)
        return ResponseEntity.ok(
            ApiResponse.success("로그인 성공", AuthResponse(accessToken, refreshTokenString))
        )
    }

    // 새로고침 시 토큰이 아직 유효한지 확인하고 로그인한 관리자 정보를 돌려준다.
    @GetMapping("/me")
    fun me(authentication: Authentication): ResponseEntity<ApiResponse<AdminUserDto>> {
        val user = userRepository.findByUserId(authentication.name)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("계정을 찾을 수 없습니다."))

        return ResponseEntity.ok(ApiResponse.success(data = AdminUserDto.from(user)))
    }
}
