package com.connect.service.common.guest

import com.connect.service.user.domain.UserRole
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.web.filter.OncePerRequestFilter

/**
 * 로그인 토큰이 없는 요청을 게스트 쿠키로 인증한다.
 *
 * Authorization 헤더가 있으면(만료됐더라도) 건드리지 않는다.
 * 회원이 토큰이 만료된 채로 요청했는데 예전 게스트 쿠키가 남아 있다고 게스트로 글이 써지면 안 되기 때문이다.
 *
 * 인증에 성공하면 authentication.name 이 게스트 userId 가 되므로,
 * 컨트롤러는 회원/게스트 구분 없이 같은 방식으로 작성자를 정할 수 있다.
 */
class GuestCookieAuthenticationFilter(
    private val guestCookieManager: GuestCookieManager,
    private val userDetailsService: UserDetailsService,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (request.getHeader("Authorization") == null &&
            SecurityContextHolder.getContext().authentication == null
        ) {
            guestCookieManager.readGuestUserId(request)?.let { guestUserId ->
                val user = try {
                    userDetailsService.loadUserByUsername(guestUserId)
                } catch (e: Exception) {
                    null
                }
                // 게스트 계정이 맞고, 관리자가 정지시키지 않은 경우에만 통과
                val isGuest = user?.authorities?.any { it.authority == UserRole.ROLE_GUEST.name } == true
                if (user != null && isGuest && user.isEnabled) {
                    SecurityContextHolder.getContext().authentication =
                        UsernamePasswordAuthenticationToken(user, "", user.authorities)
                }
            }
        }
        filterChain.doFilter(request, response)
    }
}
