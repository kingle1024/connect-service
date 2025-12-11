package com.connect.service.chatting.repository.impl

import com.connect.service.chatting.dto.ChatOneToOneRoomDto
import com.connect.service.chatting.dto.ChatRoomDto
import com.connect.service.chatting.entity.QChatRoom
import com.connect.service.chatting.entity.QRoomMembership
import com.connect.service.chatting.enums.RoomType
import com.connect.service.chatting.repository.ChatRoomRepositoryCustom
import com.querydsl.core.Tuple
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class ChatRoomRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : ChatRoomRepositoryCustom {

    // Q-클래스 인스턴스 생성
    private val chatRoom = QChatRoom.chatRoom
    private val roomMembership = QRoomMembership.roomMembership

    override fun findOneToOneRoomsByUserId(userId: String): List<ChatOneToOneRoomDto> {
        // QueryDSL로 Tuple 형태의 결과(roomId, roomName, leaderId)를 조회
        val resultTuples: List<Tuple> = queryFactory
            .select(
                chatRoom.roomId,
                chatRoom.roomName,
                roomMembership.id.userId
            )
            .from(chatRoom)
            .join(roomMembership).on(chatRoom.roomId.eq(roomMembership.id.roomId))
            .where(
                roomMembership.id.userId.ne(userId)
                    .and(chatRoom.roomType.eq(RoomType.ONE_TO_ONE.toString()))
            )
            .fetch()

        return resultTuples.map { tuple ->
            ChatOneToOneRoomDto(
                id = tuple.get(chatRoom.roomId) ?: "", // Nullable 고려
                name = tuple.get(chatRoom.roomName) ?: "",
                userId = tuple.get(roomMembership.id.userId) ?: "",
            )
        }
    }
}
