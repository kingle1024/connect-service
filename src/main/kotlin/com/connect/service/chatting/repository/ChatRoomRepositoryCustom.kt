package com.connect.service.chatting.repository

import com.connect.service.chatting.dto.ChatRoomDto

interface ChatRoomRepositoryCustom {
    fun findOneToOneRoomsByUserId(userId: String): List<ChatRoomDto>
}
