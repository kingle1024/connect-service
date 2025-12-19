package com.connect.service.chatting.repository

import com.connect.service.chatting.entity.ChatRoom
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ChatRoomRepository : JpaRepository<ChatRoom, String>, ChatRoomRepositoryCustom {
}
