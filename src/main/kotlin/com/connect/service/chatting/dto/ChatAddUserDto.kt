package com.connect.service.chatting.dto

import com.connect.service.chatting.enums.MessageType

data class ChatAddUserDto (
    val type: MessageType,
    val roomId: String,
    val sender: String,
    var roomType: String? = null,
    val roomName: String? = null,
)
