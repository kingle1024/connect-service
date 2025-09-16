package com.connect.service.board.dto

data class BoardCreateRequest(
    val title: String,
    val content: String,
    val author: String,
    val targetPlace: String
)
