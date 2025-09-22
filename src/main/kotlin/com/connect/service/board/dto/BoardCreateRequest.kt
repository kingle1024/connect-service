package com.connect.service.board.dto

import java.time.LocalDateTime

data class BoardCreateRequest(
    val title: String,
    val content: String,
    val category: String,
    val userId: String,
    val userName: String,
    val deadlineDts: LocalDateTime,
    val destination: String,
    val maxCapacity: Int,
    val currentParticipants: Int,
)
