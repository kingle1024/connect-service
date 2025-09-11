package com.connect.service.chatting

enum class MessageType {
    CHAT,
    JOIN,
    LEAVE,
    INVITE, // 새로운 멤버를 초대할 때 쓸 메시지 타입
    KICK    // 멤버를 강퇴할 때 쓸 메시지 타입
}

// 실제로 주고받을 메시지의 데이터 구조야
data class ChatMessage(
    val type: MessageType,
    val roomId: String,
    val sender: String,
    var content: String? = null,
    var recipient: String? = null,
    val roomName: String? = null
)
