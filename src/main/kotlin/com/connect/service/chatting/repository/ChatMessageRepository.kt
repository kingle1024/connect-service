package com.connect.service.chatting.repository

import com.connect.service.chatting.entity.ChatMessages
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ChatMessageRepository : JpaRepository<ChatMessages, Long> {
    // 특정 방의 모든 메시지를 시간 순서대로 조회하는 쿼리 메서드
    fun findByRoomIdOrderByTimestampAsc(roomId: String): List<ChatMessages>
}
