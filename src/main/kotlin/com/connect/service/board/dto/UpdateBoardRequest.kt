package com.connect.service.board.dto

import java.time.LocalDateTime

data class UpdateBoardRequest(
    val id: Long, // 🌟 수정할 게시글의 ID 🌟 (BoardMst의 ID 타입과 동일하게)
    val title: String,
    val content: String,
    val category: String,
    val destination: String,
    val maxCapacity: Int,
    val deadlineDts: LocalDateTime
)
