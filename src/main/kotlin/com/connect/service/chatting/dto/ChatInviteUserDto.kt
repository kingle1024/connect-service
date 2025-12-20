package com.connect.service.chatting.dto

import com.connect.service.chatting.enums.MessageType

data class ChatInviteUserDto (
    val type: MessageType,
    val roomId: String,
    val sender: String,
    var roomType: String? = null,
    var recipient: String? = null,
    val roomName: String? = null,
)
