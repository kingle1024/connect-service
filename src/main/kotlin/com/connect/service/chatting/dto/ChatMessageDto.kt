package com.connect.service.chatting.dto

import com.connect.service.chatting.enums.MessageType
import java.time.LocalDateTime

data class ChatMessageDto(
    val id: String? = null,
    val type: MessageType,
    val roomId: String,
    val sender: String,
    var content: String? = null,
    var recipient: String? = null,
    val roomName: String? = null,
    var insertDts: LocalDateTime? = null,
    // 화면에 보여줄 이름. sender/recipient 는 userId(게스트는 guest_xxx)라 사람이 읽을 이름을 따로 싣는다.
    var senderName: String? = null,
    var recipientName: String? = null,
)
