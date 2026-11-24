package com.connect.service.chatting.dto

data class ChatRoomParticipantRes (
    val roomId: String,
    val userId: String,
    // 화면에 보여줄 이름 (회원 닉네임 / 게스트 임시 닉네임). 없으면 null → 앱이 userId 로 대신 표시
    val name: String? = null,
    // 방장 여부
    val leader: Boolean = false,
)
