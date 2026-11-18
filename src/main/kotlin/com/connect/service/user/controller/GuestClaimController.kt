package com.connect.service.user.controller

import com.connect.service.common.guest.GuestCookieManager
import com.connect.service.user.service.GuestClaimService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * 게스트 기록 가져오기. (/api/account 하위는 permitAll 이고 로그인 여부는 여기서 확인한다)
 *
 * 회원은 로그인 토큰으로, 가져올 게스트는 같은 기기의 게스트 쿠키 또는 X-Guest-Token 헤더로 알아낸다.
 *  GET  /api/account/guest-claim : 가져올 수 있는 기록 요약 (로그인 직후 확인 창, 마이페이지 버튼 노출용)
 *  POST /api/account/guest-claim : 실제로 가져오기
 */
@RestController
@RequestMapping("/api/account/guest-claim")
class GuestClaimController(
    private val guestClaimService: GuestClaimService,
    private val guestCookieManager: GuestCookieManager,
) {

    @GetMapping
    fun summary(request: HttpServletRequest, authentication: Authentication?): ResponseEntity<GuestClaimService.Summary> =
        ResponseEntity.ok(guestClaimService.summary(requireLogin(authentication), guestCookieManager.readGuestUserId(request)))

    @PostMapping
    fun claim(request: HttpServletRequest, authentication: Authentication?): ResponseEntity<GuestClaimService.Summary> =
        ResponseEntity.ok(guestClaimService.claim(requireLogin(authentication), guestCookieManager.readGuestUserId(request)))

    private fun requireLogin(authentication: Authentication?): String =
        authentication?.name ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.")
}
