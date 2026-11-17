package com.connect.service.user.controller

import com.connect.service.common.guest.GuestCookieManager
import com.connect.service.user.domain.Users
import com.connect.service.user.dto.UserInfoResponse
import com.connect.service.user.service.GuestService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 비로그인 게스트 신원 API. (/api/auth 하위 경로는 SecurityConfig 에서 permitAll)
 *
 *  GET  /api/auth/guest : 쿠키에 게스트가 있으면 그 정보를, 없으면 204.  (계정을 새로 만들지 않는다)
 *  POST /api/auth/guest : 쿠키의 게스트를 돌려주고, 없으면 랜덤 닉네임으로 새로 만들어 쿠키를 심는다.
 *
 * 앱은 처음 열 때 GET 으로 기존 게스트를 복원하고, 글/댓글/채팅을 처음 쓰려는 순간에만 POST 한다.
 * (구경만 하는 방문자마다 계정이 만들어지지 않게)
 */
@RestController
@RequestMapping("/api/auth/guest")
class GuestController(
    private val guestService: GuestService,
    private val guestCookieManager: GuestCookieManager,
) {

    @GetMapping
    fun current(request: HttpServletRequest): ResponseEntity<UserInfoResponse> {
        val guest = currentGuest(request) ?: return ResponseEntity.noContent().build()
        return ResponseEntity.ok(toResponse(guest))
    }

    @PostMapping
    fun getOrCreate(request: HttpServletRequest, response: HttpServletResponse): ResponseEntity<UserInfoResponse> {
        val guest = currentGuest(request) ?: guestService.createGuest()
        // 이미 있던 게스트여도 다시 심어서 만료를 연장한다
        guestCookieManager.write(response, guest.userId)
        return ResponseEntity.ok(toResponse(guest))
    }

    private fun currentGuest(request: HttpServletRequest): Users? =
        guestCookieManager.readGuestUserId(request)?.let { guestService.findActiveGuest(it) }

    private fun toResponse(guest: Users) = UserInfoResponse(
        userId = guest.userId,
        email = "",
        name = guest.name,
        profileUrl = guest.profileUrl,
        verified = false,
        guest = true,
    )
}
