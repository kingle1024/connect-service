package com.connect.service.friend.dto

// 친구 요청 응답 (수락/거절)
data class FriendRequestProcessDto(
    val status: String // "accepted" or "rejected"
)
