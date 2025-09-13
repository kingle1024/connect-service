package com.connect.service.chatting.service

import com.connect.service.chatting.dto.ChatMessageDto
import com.connect.service.chatting.entity.ChatMessages
import com.connect.service.chatting.repository.ChatMessageRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ChatMessageService(
    private val chatMessageRepository: ChatMessageRepository
) {
    @Transactional
    fun saveChatMessage(chatMessageDto: ChatMessageDto): ChatMessages {
        val messageEntity = ChatMessages(
            type = chatMessageDto.type,
            roomId = chatMessageDto.roomId,
            sender = chatMessageDto.sender,
            content = chatMessageDto.content,
            recipient = chatMessageDto.recipient,
            timestamp = LocalDateTime.now() // 저장 시점의 시간을 명확히 기록
        )
        return chatMessageRepository.save(messageEntity)
    }

    @Transactional(readOnly = true)
    fun getChatHistoryForRoom(roomId: String): List<ChatMessageDto> {
        return chatMessageRepository.findByRoomIdOrderByTimestampAsc(roomId).map { entity ->
            ChatMessageDto(
                id = entity.id.toString(),
                type = entity.type,
                roomId = entity.roomId,
                sender = entity.sender,
                content = entity.content,
                recipient = entity.recipient
            )
        }
    }
}
