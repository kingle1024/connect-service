package com.connect.service.common.guest

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.util.Base64

class GuestCookieManagerTest {

    private val secret = Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })
    private val manager = GuestCookieManager(secret, "connect_guest", 365, true, "None")

    private fun issue(userId: String): String = manager.write(MockHttpServletResponse(), userId)

    @Test
    fun `쿠키를 심고 같은 토큰을 돌려준다`() {
        val response = MockHttpServletResponse()
        val token = manager.write(response, "guest_a")
        val setCookie = response.getHeader("Set-Cookie")!!
        assertTrue(setCookie.startsWith("connect_guest=$token"))
        assertTrue(setCookie.contains("HttpOnly") && setCookie.contains("Secure") && setCookie.contains("SameSite=None"))
    }

    @Test
    fun `쿠키가 없으면 X-Guest-Token 헤더로 식별한다 (Safari ITP)`() {
        val request = MockHttpServletRequest().apply { addHeader("X-Guest-Token", issue("guest_a")) }
        assertEquals("guest_a", manager.readGuestUserId(request))
    }

    @Test
    fun `쿠키와 헤더가 둘 다 있으면 쿠키가 우선이다`() {
        val request = MockHttpServletRequest().apply {
            setCookies(Cookie("connect_guest", issue("guest_cookie")))
            addHeader("X-Guest-Token", issue("guest_header"))
        }
        assertEquals("guest_cookie", manager.readGuestUserId(request))
    }

    @Test
    fun `쿠키가 무효면 헤더로 넘어간다`() {
        val request = MockHttpServletRequest().apply {
            setCookies(Cookie("connect_guest", "garbage"))
            addHeader("X-Guest-Token", issue("guest_header"))
        }
        assertEquals("guest_header", manager.readGuestUserId(request))
    }

    @Test
    fun `위조 토큰이나 게스트 타입이 아닌 토큰은 거부한다`() {
        val forged = issue("guest_a").dropLast(2) + "xx"
        assertNull(manager.readGuestUserId(MockHttpServletRequest().apply { addHeader("X-Guest-Token", forged) }))

        // 같은 키로 서명했어도 typ=guest 가 없으면(예: 로그인 토큰) 게스트로 인정하지 않는다
        val key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret))
        val accessLike = Jwts.builder().setSubject("member1").signWith(key, SignatureAlgorithm.HS256).compact()
        assertNull(manager.readGuestUserId(MockHttpServletRequest().apply { addHeader("X-Guest-Token", accessLike) }))
    }
}
