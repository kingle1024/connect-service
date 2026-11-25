package com.connect.service.chatting.repository.impl

import com.connect.service.chatting.dto.ChatOneToOneRoomDto
import com.connect.service.chatting.entity.QChatRoom
import com.connect.service.chatting.entity.QRoomMembership
import com.connect.service.chatting.enums.RoomType
import com.connect.service.chatting.repository.ChatRoomRepositoryCustom
import com.querydsl.core.Tuple
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class ChatRoomRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : ChatRoomRepositoryCustom {

    // Q-클래스 인스턴스 생성
    private val chatRoom = QChatRoom.chatRoom
    private val roomMembership = QRoomMembership.roomMembership

    /**
     * 내가 들어 있는 1:1 방 목록 (상대 userId 와 함께).
     *
     * - 내가 멤버인 방만 본다 (예전에는 남들끼리의 1:1 방까지 섞여 나와 엉뚱한 방으로 들어갈 수 있었다)
     * - 1:1 로 만들었어도 누군가 초대돼 3명 이상이 된 방은 이제 단체방이라 뺀다
     *   (참여자 프로필의 '대화하기'가 지금 있는 단체방으로 다시 들어가던 문제)
     */
    override fun findOneToOneRoomsByUserId(userId: String): List<ChatOneToOneRoomDto> {
        val myMembership = QRoomMembership("myMembership")
        val myRoomIds = JPAExpressions
            .select(myMembership.id.roomId)
            .from(myMembership)
            .where(myMembership.id.userId.eq(userId))

        val rows: List<Tuple> = queryFactory
            .select(chatRoom.roomId, chatRoom.roomName, roomMembership.id.userId)
            .from(chatRoom)
            .join(roomMembership).on(chatRoom.roomId.eq(roomMembership.id.roomId))
            .where(
                chatRoom.roomType.eq(RoomType.ONE_TO_ONE.toString())
                    .and(chatRoom.roomId.`in`(myRoomIds))
                    .and(roomMembership.id.userId.ne(userId))
            )
            .fetch()

        // 나 말고 딱 한 명만 있는 방만 남긴다
        return rows
            .groupBy { it.get(chatRoom.roomId) ?: "" }
            .filter { (roomId, members) -> roomId.isNotBlank() && members.size == 1 }
            .map { (roomId, members) ->
                val row = members.first()
                ChatOneToOneRoomDto(
                    name = row.get(chatRoom.roomName) ?: "",
                    userId = row.get(roomMembership.id.userId) ?: "",
                    roomId = roomId,
                )
            }
    }
}
