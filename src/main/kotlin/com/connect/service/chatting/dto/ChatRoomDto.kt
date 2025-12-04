package com.connect.service.chatting.dto

data class ChatRoomDto(
    val id: String,
    val name: String,
    val leaderId: String,
    val participantsCount: Long
)
