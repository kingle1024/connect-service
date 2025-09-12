package com.connect.service.chatting.dto

import com.connect.service.chatting.enums.MessageType

data class ChatMessage(
    val type: MessageType,
    val roomId: String,
    val sender: String,
    var content: String? = null,
    var recipient: String? = null,
    val roomName: String? = null
)
