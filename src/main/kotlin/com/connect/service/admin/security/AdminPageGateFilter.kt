package com.connect.service.admin.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.filter.OncePerRequestFilter
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * 백오피스 정적 화면(/admin 아래) 앞에 거는 비밀번호 문.
 *
 * 브라우저 기본 인증(HTTP Basic) 창을 띄워 비밀번호가 맞을 때만 화면 파일을 내려준다.
 * 아이디 칸은 아무 값이나 넣어도 되고 비밀번호만 본다.
 * 실제 데이터(/api/admin 아래 API)는 여기와 별개로 관리자 로그인(JWT, ROLE_ADMIN)이 지킨다.
 *
 * 저장소가 공개라서 비밀번호 원문은 두지 않고 BCrypt 해시만 설정값으로 받는다.
 * 해시가 비어 있으면 문을 열어 둔다(로컬 개발용).
 */
class AdminPageGateFilter(
    private val passwordHash: String,
    private val encoder: BCryptPasswordEncoder = BCryptPasswordEncoder()
) : OncePerRequestFilter() {

    // 화면 하나를 열 때 html/css/js 로 요청이 여러 번 오므로, 통과한 헤더는 기억해서 BCrypt 를 반복하지 않는다.
    @Volatile
    private var lastAcceptedHeader: String? = null

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        if (passwordHash.isBlank() || isAllowed(request.getHeader("Authorization"))) {
            chain.doFilter(request, response)
            return
        }
        response.setHeader("WWW-Authenticate", "Basic realm=\"connect admin\", charset=\"UTF-8\"")
        response.setHeader("Cache-Control", "no-store")
        response.status = HttpServletResponse.SC_UNAUTHORIZED
    }

    private fun isAllowed(header: String?): Boolean {
        if (header == null || !header.startsWith("Basic ")) return false
        if (header == lastAcceptedHeader) return true
        val decoded = try {
            String(Base64.getDecoder().decode(header.removePrefix("Basic ").trim()), StandardCharsets.UTF_8)
        } catch (e: IllegalArgumentException) {
            return false
        }
        val password = decoded.substringAfter(':', "")
        val ok = password.isNotEmpty() && encoder.matches(password, passwordHash)
        if (ok) lastAcceptedHeader = header
        return ok
    }
}
