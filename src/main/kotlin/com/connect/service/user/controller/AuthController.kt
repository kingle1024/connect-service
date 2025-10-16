package com.connect.service.user.controller

import com.connect.service.common.jwt.JwtTokenProvider
import com.connect.service.common.jwt.dto.RefreshToken
import com.connect.service.common.jwt.repository.RefreshTokenRepository
import com.connect.service.user.domain.CustomUserDetails
import com.connect.service.user.domain.Users
import com.connect.service.user.dto.AuthResponse
import com.connect.service.user.dto.LoginRequest
import com.connect.service.user.dto.RegistUserRequest
import com.connect.service.user.dto.TokenRefreshRequest
import com.connect.service.user.dto.UserInfoResponse
import com.connect.service.user.repository.UserRepository
import com.connect.service.user.service.CustomUserDetailsService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.ZoneId
import mu.KotlinLogging

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authenticationManager: AuthenticationManager,
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val customUserDetailsService: CustomUserDetailsService,
    private val refreshTokenRepository: RefreshTokenRepository
) {
    companion object {
        private val log = KotlinLogging.logger {} // 클래스 레벨에서 로거를 초기화합니다.
    }

    @PostMapping("/register")
    fun registerUser(@RequestBody request: RegistUserRequest): ResponseEntity<String> {
        if (userRepository.findByUserId(request.userId) != null) {
            return ResponseEntity.badRequest().body("User ID already taken!")
        }

        val newUsers = Users(
            userId = request.userId,
            email = request.email,
            name = request.name,
            rawPassword = passwordEncoder.encode(request.password)
        )
        userRepository.save(newUsers)
        return ResponseEntity.ok("User registered successfully!")
    }

    // 로그인 API: Access Token 및 Refresh Token 발급
    @PostMapping("/login")
    fun authenticateUser(
        @RequestBody loginRequest: LoginRequest,
        request: HttpServletRequest
    ): ResponseEntity<AuthResponse> {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(loginRequest.userId, loginRequest.password)
        )

        SecurityContextHolder.getContext().authentication = authentication

        val accessToken = jwtTokenProvider.createAccessToken(authentication)
        val (refreshTokenString, refreshTokenExpiresAt) = jwtTokenProvider.createRefreshToken(authentication)

        val newRefreshTokenEntity = RefreshToken(
            userId = authentication.name,
            token = refreshTokenString,
            issuedAt = LocalDateTime.now(),
            expiresAt = refreshTokenExpiresAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
            ipAddress = request.remoteAddr,
            userAgent = request.getHeader("User-Agent")
        )
        refreshTokenRepository.save(newRefreshTokenEntity)

        return ResponseEntity.ok(AuthResponse(accessToken, refreshTokenString))
    }

    // Access Token 재발급 API: Refresh Token 유효성 검증 후 새 Access Token 발급
    @PostMapping("/refresh-token")
    fun refreshAccessToken(
        @RequestBody request: TokenRefreshRequest,
        httpRequest: HttpServletRequest // HTTP 요청 정보를 받기 위해 추가
    ): ResponseEntity<AuthResponse> {
        // 1. Refresh Token 문자열 자체의 유효성 (서명, 구조 등) 검증
        if (!jwtTokenProvider.validateToken(request.refreshToken)) {
            // 토큰이 유효하지 않거나 변조된 경우
            return ResponseEntity.badRequest().body(AuthResponse("", "", "Invalid refresh token"))
        }

        // 2. Refresh Token을 DB에서 조회
        val storedRefreshToken = refreshTokenRepository.findByToken(request.refreshToken)
            ?: return ResponseEntity.badRequest().body(AuthResponse("", "", "Refresh token not found"))

        // 3. DB에 저장된 Refresh Token의 상태 검증
        if (storedRefreshToken.isRevoked || storedRefreshToken.expiresAt.isBefore(LocalDateTime.now())) {
            // 이미 무효화되었거나 만료된 토큰
            return ResponseEntity.badRequest().body(AuthResponse("", "", "Expired or revoked refresh token"))
        }

        // 4. 토큰에서 사용자 ID 추출 및 DB의 사용자 ID와 비교 (추가 보안)
        val userIdFromToken = jwtTokenProvider.getUserIdFromToken(request.refreshToken)
        if (storedRefreshToken.userId != userIdFromToken) {
            // 토큰에 명시된 사용자 ID와 DB에 저장된 사용자 ID가 일치하지 않는 경우
            // 이 시나리오는 극히 드물거나 Refresh Token Rotation을 잘못 구현했을 때 발생할 수 있습니다.
            return ResponseEntity.badRequest().body(AuthResponse("", "", "Mismatched user for refresh token"))
        }

        // 5. 기존 Refresh Token 무효화 (Refresh Token Rotation)
        storedRefreshToken.isRevoked = true
        storedRefreshToken.revokedAt = LocalDateTime.now()
        refreshTokenRepository.save(storedRefreshToken)

        // 6. 사용자 정보를 다시 로드하여 새로운 Access Token 및 Refresh Token 생성
        val userDetails = customUserDetailsService.loadUserByUsername(userIdFromToken)
        val authentication = UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)

        val newAccessToken = jwtTokenProvider.createAccessToken(authentication)
        val (newRefreshTokenString, newRefreshTokenExpiresAt) = jwtTokenProvider.createRefreshToken(authentication)

        // 7. 새로운 Refresh Token을 DB에 저장
        val newRefreshTokenEntity = RefreshToken(
            userId = authentication.name,
            token = newRefreshTokenString,
            issuedAt = LocalDateTime.now(),
            expiresAt = newRefreshTokenExpiresAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
            ipAddress = httpRequest.remoteAddr,
            userAgent = httpRequest.getHeader("User-Agent")
        )
        refreshTokenRepository.save(newRefreshTokenEntity)

        return ResponseEntity.ok(AuthResponse(newAccessToken, newRefreshTokenString))
    }

    // 보호된 API (인증된 사용자만 접근 가능)
    @GetMapping("/secure-data")
    fun getSecureData(): ResponseEntity<String> {
        val authentication = SecurityContextHolder.getContext().authentication
        val username = authentication.name // 인증된 사용자의 userId
        return ResponseEntity.ok("안녕하세요, $username 님! 이 정보는 보호된 데이터입니다.")
    }

    @GetMapping("/me")
    fun getUserInfoByToken(@RequestHeader("Authorization") authorizationHeader: String): ResponseEntity<UserInfoResponse> {
        return try {
            if (!authorizationHeader.startsWith("Bearer ")) {
                log.warn("인증 헤더 형식이 잘못되었습니다: {}", authorizationHeader)
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null)
            }
            val token = authorizationHeader.substring(7)

            val authentication = jwtTokenProvider.getAuthentication(token)
            val customUserDetails = authentication.principal as CustomUserDetails

            val userInfo = UserInfoResponse(
                userId = customUserDetails.getUserId(),
                email = customUserDetails.getEmail(),
                name = customUserDetails.getName(),
                profileUrl = customUserDetails.getProfileUrl()
            )

            log.info("사용자 정보 조회 성공: userId = {}", customUserDetails.getUserId())
            ResponseEntity.ok(userInfo)
        } catch (e: Exception) {
            log.error("토큰으로 사용자 정보 조회 중 오류 발생: {}", e.message, e)
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null)
        }
    }
}
