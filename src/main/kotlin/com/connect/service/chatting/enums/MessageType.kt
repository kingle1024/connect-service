package com.connect.service.chatting.enums

enum class MessageType {
    CHAT,
    JOIN,
    LEAVE,
    INVITE, // 새로운 멤버를 초대할 때 쓸 메시지 타입
    KICK    // 멤버를 강퇴할 때 쓸 메시지 타입
}
