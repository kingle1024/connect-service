package com.connect.service.user.dto

/**
 * 게스트 신원 응답.
 * guestToken 은 쿠키에 담긴 것과 같은 서명 토큰이다. 쿠키가 막힌 브라우저에서 X-Guest-Token 헤더로 보낸다.
 */
data class GuestInfoResponse(
    val userId: String,
    val name: String,
    val profileUrl: String? = null,
    val guestToken: String,
    val email: String = "",
    val verified: Boolean = false,
    val guest: Boolean = true,
)
