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
    val type: MessageType, // 메시지 타입 (위의 enum)
    val roomId: String,    // 어떤 채팅방의 메시지인지!
    val sender: String,    // 누가 이 메시지를 보냈는지 (유저 ID나 닉네임)
    val content: String?,  // 실제 메시지 내용 (초대/강퇴 메시지는 내용이 없을 수도 있지)
    val recipient: String? = null // 초대/강퇴 시 '누구'에게 적용되는지 (초대받거나 강퇴당할 유저의 ID)
)
