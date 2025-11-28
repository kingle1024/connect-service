package com.connect.service.friend.dto

// 친구 요청을 보낼 때
data class FriendRequestSendDto(
    val receiverUserId: String // 친구 요청을 받을 사용자의 user_id (고유 식별자)
)
