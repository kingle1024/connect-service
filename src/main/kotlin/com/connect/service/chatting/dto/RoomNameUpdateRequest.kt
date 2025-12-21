package com.connect.service.chatting.dto

data class RoomNameUpdateRequest(
    val roomId: String,
    val roomName: String // 새롭게 변경할 방 이름
)
