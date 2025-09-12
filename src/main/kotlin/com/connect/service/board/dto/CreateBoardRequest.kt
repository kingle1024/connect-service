package com.connect.service.board.dto

data class CreateBoardRequest(
    val title: String,
    val content: String,
    val author: String
)
