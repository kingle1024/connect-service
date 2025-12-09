package com.connect.service.chatting.dto

import com.connect.service.chatting.enums.MessageType
import java.time.LocalDateTime

data class ChatAddUserDto (
    val id: String? = null,
    val type: MessageType,
    val roomId: String,
    val sender: String,
    var content: String? = null,
    var roomType: String,
    var recipient: String? = null,
    val roomName: String? = null,
    var insertDts: LocalDateTime? = null,
)
