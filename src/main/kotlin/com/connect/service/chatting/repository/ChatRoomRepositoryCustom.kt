package com.connect.service.chatting.repository

import com.connect.service.chatting.dto.ChatOneToOneRoomDto

interface ChatRoomRepositoryCustom {
    fun findOneToOneRoomsByUserId(userId: String): List<ChatOneToOneRoomDto>
}
