package com.connect.service.friend.dto

// 친구 목록
data class FriendResponse(
    val friendshipId: Long,
    val friendUserId: String,     // 친구의 userId (String)
    val friendNickname: String,
    val isBlocked: Boolean
)
