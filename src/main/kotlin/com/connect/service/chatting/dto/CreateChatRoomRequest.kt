package com.connect.service.chatting.dto

data class CreateChatRoomRequest(
    val roomId: String,
    val userId: String,
    val roomType: String,
    val roomName: String? = null // roomName은 optional이라서 기본값 null을 줘도 좋아요
)
