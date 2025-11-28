package com.connect.service.chatting.dto

data class SendMessageRequest(
    val roomId: Long,
    val content: String,
    val messageType: String = "text"
)
