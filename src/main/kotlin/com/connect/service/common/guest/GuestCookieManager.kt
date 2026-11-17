package com.connect.service.common.guest

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.security.Key
import java.time.Duration
import java.util.Base64
import java.util.Date

/**
 * 게스트 쿠키 발급/검증.
 *
 * 쿠키 값은 서버 비밀키(jwt.secret-key)로 서명한 JWT 다. subject 가 게스트의 userId 이고
 * typ=guest 클레임이 있어야만 게스트 쿠키로 인정한다. (로그인 토큰을 쿠키에 넣어도 통하지 않는다)
 * 서명이 있으니 다른 사람의 게스트 아이디로 바꿔 끼울 수 없다.
 *
 * HttpOnly 라 웹 페이지 스크립트가 읽지 못한다.
 * 운영 프론트(vercel)와 백엔드(cloudtype)는 서로 다른 사이트라 SameSite=None + Secure 여야 쿠키가 실린다.
 * (localhost 는 http 여도 브라우저가 Secure 쿠키를 받아준다)
 */
@Component
class GuestCookieManager(
    @Value("\${jwt.secret-key}") secretKeyString: String,
    @Value("\${app.guest.cookie-name:connect_guest}") val cookieName: String,
    @Value("\${app.guest.cookie-max-age-days:365}") private val maxAgeDays: Long,
    @Value("\${app.guest.cookie-secure:true}") private val secure: Boolean,
    @Value("\${app.guest.cookie-same-site:None}") private val sameSite: String,
) {
    private val secretKey: Key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretKeyString))

    companion object {
        private const val TYPE_CLAIM = "typ"
        private const val GUEST_TYPE = "guest"
    }

    /** 응답에 게스트 쿠키를 심는다. */
    fun write(response: HttpServletResponse, guestUserId: String) {
        val now = Date()
        val maxAge = Duration.ofDays(maxAgeDays)
        val token = Jwts.builder()
            .setSubject(guestUserId)
            .claim(TYPE_CLAIM, GUEST_TYPE)
            .setIssuedAt(now)
            .setExpiration(Date(now.time + maxAge.toMillis()))
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact()

        val cookie = ResponseCookie.from(cookieName, token)
            .httpOnly(true)
            .secure(secure)
            .sameSite(sameSite)
            .path("/")
            .maxAge(maxAge)
            .build()
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }

    /** 요청의 게스트 쿠키에서 게스트 userId 를 꺼낸다. 없거나 위조·만료면 null. */
    fun readGuestUserId(request: HttpServletRequest): String? {
        val token = request.cookies?.firstOrNull { it.name == cookieName }?.value ?: return null
        return try {
            val claims = Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).body
            if (claims[TYPE_CLAIM] != GUEST_TYPE) null else claims.subject
        } catch (e: Exception) {
            null
        }
    }
}
