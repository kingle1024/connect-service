package com.connect.service.chatting

import java.util.concurrent.ConcurrentHashMap

data class ChatRoom(
    val id: String, // 채팅방 ID
    val roomLeader: String, // 방장 유저 ID! 이게 중요하지!
    // 채팅방 참여자 목록 (Set으로 중복 방지)
    val participants: ConcurrentHashMap<String, String> = ConcurrentHashMap()
) {
    // 참여자 추가
    fun addParticipant(userId: String): Boolean {
        return participants.put(userId, userId) == null // 새로 추가되면 true
    }

    // 참여자 제거
    fun removeParticipant(userId: String): Boolean {
        return participants.remove(userId) != null // 제거되면 true
    }

    // 특정 유저가 참여자인지 확인
    fun hasParticipant(userId: String): Boolean {
        return participants.containsKey(userId)
    }
}
