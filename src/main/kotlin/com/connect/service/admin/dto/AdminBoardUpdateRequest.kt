package com.connect.service.admin.dto

import java.time.LocalDateTime

data class AdminBoardUpdateRequest(
    val title: String,
    val content: String,
    val category: String,
    val destination: String,
    val maxCapacity: Int,
    val deadlineDts: LocalDateTime
)
