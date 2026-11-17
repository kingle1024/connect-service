package com.connect.service.board.dto

import java.time.LocalDateTime

data class BoardCreateRequest(
    val title: String,
    val content: String,
    val category: String,
    val userId: String = "", // 무시됨: 작성자는 인증(로그인 토큰/게스트 쿠키)으로 정한다
    val userName: String = "", // 무시됨: 작성자 이름도 서버의 계정 정보로 채운다
    val deadlineDts: LocalDateTime,
    val destination: String,
    val maxCapacity: Int,
    val currentParticipants: Int,
)
