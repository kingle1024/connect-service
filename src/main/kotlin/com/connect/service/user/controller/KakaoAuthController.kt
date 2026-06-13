package com.connect.service.user.controller

import com.connect.service.common.jwt.JwtTokenProvider
import com.connect.service.common.jwt.dto.RefreshToken
import com.connect.service.common.jwt.repository.RefreshTokenRepository
import com.connect.service.user.service.KakaoOAuthService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Base64

@RestController
@RequestMapping("/api/auth/kakao")
class KakaoAuthController(
    private val kakaoOAuthService: KakaoOAuthService,
    private val jwtTokenProvider: JwtTokenProvider,
    private val refreshTokenRepository: RefreshTokenRepository
) {
    companion object {
        private val log = KotlinLogging.logger {}
    }

    @Value("\${spring.security.oauth2.client.registration.kakao.client-id}")
    private lateinit var clientId: String

    @Value("\${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private lateinit var redirectUri: String

    @Value("\${spring.security.oauth2.client.registration.kakao.scope}")
    private lateinit var scope: String

    @Value("\${spring.security.oauth2.client.provider.kakao.authorization-uri}")
    private lateinit var authorizationUri: String

    // 앱으로 돌려보낼 수 있는 리다이렉트 URI 화이트리스트 (오픈 리다이렉트 방지)
    @Value("\${app.oauth2.allowed-redirect-uris}")
    private lateinit var allowedRedirectUris: String

    // 1단계: 프론트가 호출 → 카카오 인가 페이지로 302 리다이렉트
    // redirectUri: 로그인 완료 후 토큰을 전달받을 앱의 주소(딥링크/웹 origin)
    @GetMapping("/authorize")
    fun authorize(
        @RequestParam("redirectUri") appRedirectUri: String,
        response: HttpServletResponse
    ) {
        if (!isAllowedRedirect(appRedirectUri)) {
            log.warn("허용되지 않은 redirectUri 요청: {}", appRedirectUri)
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "허용되지 않은 redirectUri 입니다.")
            return
        }

        // 앱의 최종 리다이렉트 주소를 state에 실어 카카오 왕복 후 콜백에서 복원
        val state = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(appRedirectUri.toByteArray(StandardCharsets.UTF_8))

        val kakaoAuthUrl = buildString {
            append(authorizationUri)
            append("?client_id=").append(clientId)
            append("&redirect_uri=").append(encode(redirectUri))
            append("&response_type=code")
            append("&scope=").append(encode(scope))
            append("&state=").append(state)
        }

        response.sendRedirect(kakaoAuthUrl)
    }

    // 2단계: 카카오가 인가 코드와 함께 호출하는 콜백
    // 토큰 교환 → 사용자 조회/생성 → JWT 발급 → 앱 주소로 토큰 리다이렉트
    @GetMapping("/callback")
    fun callback(
        @RequestParam("code") code: String,
        @RequestParam("state") state: String,
        request: HttpServletRequest,
        response: HttpServletResponse
    ) {
        val appRedirectUri = try {
            String(Base64.getUrlDecoder().decode(state), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            log.warn("state 디코딩 실패: {}", state)
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "잘못된 state 입니다.")
            return
        }

        if (!isAllowedRedirect(appRedirectUri)) {
            log.warn("허용되지 않은 redirectUri 콜백: {}", appRedirectUri)
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "허용되지 않은 redirectUri 입니다.")
            return
        }

        // 카카오 토큰 교환 및 사용자 정보 조회
        val kakaoAccessToken = kakaoOAuthService.exchangeCodeForToken(code)
        val kakaoUser = kakaoOAuthService.fetchKakaoUser(kakaoAccessToken)
        val user = kakaoOAuthService.loginOrRegister(kakaoUser)

        // 우리 서비스의 JWT(Access/Refresh) 발급 (일반 로그인과 동일한 흐름)
        val authentication = UsernamePasswordAuthenticationToken(user, null, user.authorities)
        val accessToken = jwtTokenProvider.createAccessToken(authentication)
        val (refreshTokenString, refreshTokenExpiresAt) = jwtTokenProvider.createRefreshToken(authentication)

        val newRefreshTokenEntity = RefreshToken(
            userId = user.userId,
            token = refreshTokenString,
            issuedAt = LocalDateTime.now(),
            expiresAt = refreshTokenExpiresAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
            ipAddress = request.remoteAddr,
            userAgent = request.getHeader("User-Agent")
        )
        refreshTokenRepository.save(newRefreshTokenEntity)

        // 앱 주소로 토큰 전달 (쿼리 파라미터)
        val separator = if (appRedirectUri.contains("?")) "&" else "?"
        val targetUrl = "$appRedirectUri${separator}accessToken=${encode(accessToken)}&refreshToken=${encode(refreshTokenString)}"
        response.sendRedirect(targetUrl)
    }

    // 화이트리스트에 등록된 접두사로 시작하는지 검증
    private fun isAllowedRedirect(uri: String): Boolean {
        return allowedRedirectUris.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .any { uri.startsWith(it) }
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8)
}
