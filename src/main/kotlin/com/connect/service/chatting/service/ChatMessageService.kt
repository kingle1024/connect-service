package com.connect.service.chatting.service

import com.connect.service.chatting.dto.ChatMessageDto
import com.connect.service.chatting.entity.ChatMessages
import com.connect.service.chatting.repository.ChatMessageRepository
import com.connect.service.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ChatMessageService(
    private val chatMessageRepository: ChatMessageRepository,
    private val userRepository: UserRepository,
) {
    /** userId → 화면 표시 이름(users.name). 이름이 없거나 모르는 사용자는 빠진다. */
    @Transactional(readOnly = true)
    fun displayNames(userIds: Collection<String?>): Map<String, String> {
        val ids = userIds.filterNotNull().filter { it.isNotBlank() }.distinct()
        if (ids.isEmpty()) return emptyMap()
        return userRepository.findByUserIdIn(ids)
            .filter { it.name.isNotBlank() }
            .associate { it.userId to it.name }
    }

    /** sender/recipient 의 표시 이름을 채운다. */
    fun withNames(dto: ChatMessageDto): ChatMessageDto {
        val names = displayNames(listOf(dto.sender, dto.recipient))
        dto.senderName = names[dto.sender]
        dto.recipientName = dto.recipient?.let { names[it] }
        return dto
    }

    @Transactional
    fun saveChatMessage(chatMessageDto: ChatMessageDto): ChatMessages {
        val messageEntity = ChatMessages(
            type = chatMessageDto.type,
            roomId = chatMessageDto.roomId,
            sender = chatMessageDto.sender,
            content = chatMessageDto.content,
            recipient = chatMessageDto.recipient,
            insertDts = LocalDateTime.now() // 저장 시점의 시간을 명확히 기록
        )
        return chatMessageRepository.save(messageEntity)
    }

    @Transactional(readOnly = true)
    fun getChatHistoryForRoom(roomId: String): List<ChatMessageDto> {
        val entities = chatMessageRepository.findByRoomIdOrderByInsertDtsAsc(roomId)
        val names = displayNames(entities.flatMap { listOf(it.sender, it.recipient) })
        return entities.map { entity ->
            ChatMessageDto(
                id = entity.id.toString(),
                type = entity.type,
                roomId = entity.roomId,
                sender = entity.sender,
                content = entity.content,
                recipient = entity.recipient,
                insertDts = entity.insertDts,
                senderName = names[entity.sender],
                recipientName = entity.recipient?.let { names[it] },
            )
        }
    }
}
