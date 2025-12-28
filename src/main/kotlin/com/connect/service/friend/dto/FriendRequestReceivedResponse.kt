package com.connect.service.friend.dto

// 받은 친구 요청 목록
data class FriendRequestReceivedResponse(
    val id: Long,
    val name: String,
    val senderId: String,
    val receiverId: String,
    val status: String,
)
