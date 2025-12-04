package com.connect.service.chatting.repository.impl

import com.connect.service.chatting.dto.ChatRoomDto
import com.connect.service.chatting.entity.QChatRoom
import com.connect.service.chatting.entity.QRoomMembership
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

    override fun findOneToOneRoomsByUserId(userId: String): List<ChatRoomDto> {
        // QueryDSL로 Tuple 형태의 결과(roomId, roomName, leaderId)를 조회
        val resultTuples: List<Tuple> = queryFactory
            .select(
                chatRoom.roomId,
                chatRoom.roomName,
                chatRoom.leaderUserId
            )
            .from(chatRoom)
            .join(roomMembership).on(chatRoom.roomId.eq(roomMembership.id.roomId))
            .where(roomMembership.id.userId.eq(userId))
            .groupBy(chatRoom.roomId, chatRoom.roomName, chatRoom.leaderUserId)
            .having(roomMembership.id.roomId.count().eq(2L))
            .fetch()

        // 조회된 Tuple을 ChatRoomDto로 매핑하면서 participantsCount를 2L로 할당
        return resultTuples.map { tuple ->
            ChatRoomDto(
                id = tuple.get(chatRoom.roomId) ?: "", // Nullable 고려
                name = tuple.get(chatRoom.roomName) ?: "",
                leaderId = tuple.get(chatRoom.leaderUserId) ?: "",
                participantsCount = 2L // 여기서 직접 2L을 할당
            )
        }
    }
}
