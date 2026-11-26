package com.connect.service.chatting.controller

import com.connect.service.board.service.BoardChatService
import com.connect.service.chatting.dto.ChatAddUserDto
import com.connect.service.chatting.dto.ChatInviteUserDto
import com.connect.service.chatting.dto.ChatMessageDto
import com.connect.service.chatting.enums.MessageType
import com.connect.service.chatting.enums.RoomType
import com.connect.service.chatting.service.ChatMessageService
import com.connect.service.chatting.service.ChatRoomService
import com.connect.service.chatting.repository.RoomMembershipRepository
import com.connect.service.user.service.CustomUserDetailsService
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import java.time.LocalDateTime
import kotlin.collections.set

@Controller
class ChatWebSocketController (
    private val messagingTemplate: SimpMessagingTemplate,
    private val chatRoomService: ChatRoomService,
    private val chatMessageService: ChatMessageService,
    private val userService: CustomUserDetailsService,
    private val roomMembershipRepository: RoomMembershipRepository,
    private val boardChatService: BoardChatService,
) {
    /**
     * 방장은 방을 나갈 수 없다.
     *  - 모집 채팅방: 글이 살아 있는 동안은 혼자 남아 있어도 못 나간다 (글을 지우면 나갈 수 있다)
     *  - 그 밖의 방: 다른 사람이 남아 있으면 못 나간다. 혼자 남은 방은 나가면 방이 지워진다.
     */
    private fun leaderCannotLeave(roomId: String, userId: String): Boolean =
        chatRoomService.isRoomLeader(roomId, userId) &&
            (boardChatService.isActiveBoardRoom(roomId) || roomMembershipRepository.countByIdRoomId(roomId) > 1)

    @MessageMapping("/chat.sendMessage")
    fun sendMessage(@Payload chatMessageDto: ChatMessageDto) {
        val roomId = chatMessageDto.roomId
        val sender = chatMessageDto.sender
        val content = chatMessageDto.content

        // 방이 존재하는지 확인 (선택 사항)
        if (!chatRoomService.doesRoomExist(roomId)) {
             messagingTemplate.convertAndSendToUser(
                sender, "/queue/errors", "채팅방 ${roomId}는 존재하지 않습니다.")
             return
        }

        println("메시지 보냄 - 방: $roomId, 발신자: $sender, 내용: $content")

        val savedDto = chatMessageService.saveChatMessage(ChatMessageDto( // DTO를 서비스로 넘겨서 저장
            type = chatMessageDto.type,
            roomId = chatMessageDto.roomId,
            sender = chatMessageDto.sender,
            content = chatMessageDto.content,
            insertDts = LocalDateTime.now() // 현재 시간으로 저장
        ))

        println("메시지 보냄 및 저장 완료 - ID: ${savedDto.id}, 방: ${savedDto.roomId}, 발신자: ${savedDto.sender}, 내용: ${savedDto.content}, 시간: ${savedDto.insertDts}")

        val outgoing = chatMessageService.withNames(ChatMessageDto(
            id = savedDto.id.toString(),
            type = savedDto.type,
            roomId = savedDto.roomId,
            sender = savedDto.sender,
            content = savedDto.content,
            recipient = savedDto.recipient,
            insertDts = savedDto.insertDts,
        ))
        messagingTemplate.convertAndSend("/topic/chat/$roomId", outgoing)
        notifyMembers(outgoing)
    }

    /**
     * 방의 다른 멤버들에게 '새 메시지' 알림을 보낸다 (/topic/notify/{userId}).
     * 앱은 채팅방 밖에 있어도 이걸 받아 브라우저 알림을 띄운다.
     */
    private fun notifyMembers(message: ChatMessageDto) {
        if (message.type != MessageType.CHAT) return
        try {
            val room = chatRoomService.getRoom(message.roomId) ?: return
            val members = roomMembershipRepository.findByIdRoomId(message.roomId).map { it.id.userId }
            val payload = mapOf(
                "roomId" to message.roomId,
                "roomName" to room.roomName,
                "roomType" to room.roomType,
                "memberCount" to members.size,
                "messageId" to message.id,
                "sender" to message.sender,
                "senderName" to (message.senderName ?: message.sender),
                "content" to message.content,
            )
            members.filter { it != message.sender }.forEach { userId ->
                messagingTemplate.convertAndSend("/topic/notify/$userId", payload)
            }
        } catch (e: Exception) {
            println("새 메시지 알림 전송 실패 - 방: ${message.roomId}, ${e.message}")
        }
    }

    @MessageMapping("/chat.addUser")
    fun addUser(@Payload chatAddUser: ChatAddUserDto, headerAccessor: SimpMessageHeaderAccessor) {
        val userId = chatAddUser.sender // 메시지 보낸 사람 = 입장하는 유저
        val roomId = chatAddUser.roomId
        val roomName = chatAddUser.roomName
        val roomType = chatAddUser.roomType?: RoomType.GROUP

        headerAccessor.sessionAttributes!!["username"] = userId
        headerAccessor.sessionAttributes!!["roomId"] = roomId
        println("유저 입장 - 방: $roomId, 유저: $userId")

        val added = chatRoomService.addParticipant(roomId, userId, roomName, roomType.toString())

        if (added) {
            println("유저 추가됨 - 방: $roomId, 유저: $userId, 방 이름: $roomName")
            messagingTemplate.convertAndSend("/topic/chat/$roomId",
                chatMessageService.withNames(ChatMessageDto(null, MessageType.JOIN, roomId, userId, "${userId}님이 입장했습니다.", insertDts = LocalDateTime.now()))
            )
        } else {
            // 이미 참여 중인 방에 다시 들어온 것. 채팅방을 열 때마다 addUser 가 오므로 정상 흐름이다.
            // 예전에는 여기서 "/queue/errors" 로 오류를 보냈는데, 웹소켓에 인증 주체가 없어 아무도 받지 못했다.
            // 게스트 쿠키가 핸드셰이크를 인증하면서 게스트에게는 이 메시지가 전달돼, 방을 열 때마다
            // "이미 방에 입장해 있습니다" 오류 팝업이 떴다. 오류가 아니므로 보내지 않는다.
            println("이미 참여 중인 방 재입장 - 방: $roomId, 유저: $userId")
        }
    }

    @MessageMapping("/chat.inviteUser")
    fun inviteUser(@Payload chatInviteUser: ChatInviteUserDto) {
        val sender = chatInviteUser.sender // 초대하는 사람
        val recipient = chatInviteUser.recipient // 초대받는 사람
        val roomId = chatInviteUser.roomId

        if (!chatRoomService.isRoomLeader(roomId, sender)) {
            messagingTemplate.convertAndSendToUser(
                sender, "/queue/errors", "당신은 ${roomId} 방장이 아닙니다."
            )
            return
        }
        if (!chatRoomService.doesRoomExist(roomId)) {
             messagingTemplate.convertAndSendToUser(
                sender, "/queue/errors", "채팅방 ${roomId}는 존재하지 않습니다.")
             return
        }
        // 방 이름
        val chatRoom = chatRoomService.findByRoomId(roomId)
        val roomName: String = if (chatRoom?.roomName.isNullOrBlank()) {
            // chatRoom?.roomName이 null이거나 비어있을 경우 (또는 chatRoom 자체가 null일 경우)
            val userIds = chatRoomService.getRoomsForRoomId(roomId)
            val userIdList: MutableList<String> = mutableListOf()
            for (user in userIds) {
                userIdList.add(user.userId)
            }
            val userItems = userService.getRoomMemberShipByUserIds(userIdList)
            userItems.map { it.value.name }.joinToString(",")
        } else {
            chatRoom!!.roomName!! // chatRoom 자체도 non-null, 그 안의 roomName도 non-null임을 명시
        }

        val addedToRoom = chatRoomService.addParticipant(roomId, recipient!!, chatRoom?.roomName, chatRoom?.roomType)

        if (addedToRoom) { // 성공적으로 추가되었다면 (새로 멤버가 되었다면)
            // 초대받는 사람에게 초대 알림 메시지 발송
            val invitationJson = mapOf(
                "sender" to sender,
                "roomId" to roomId,
                "roomName" to roomName,
                "message" to "${sender}님이 방으로 당신을 초대했습니다."
            )
            messagingTemplate.convertAndSendToUser(
                recipient, // 받는 사람의 User Queue로 메시지 전송
                "/queue/invitations",
                invitationJson
            )

            // 모든 방 참가자에게 누가 누구를 초대했는지 알림 (채팅방 내 시스템 메시지)
            messagingTemplate.convertAndSend("/topic/chat/$roomId",
                chatMessageService.withNames(ChatMessageDto(null, MessageType.INVITE, roomId, sender, recipient = recipient, roomName = "1234", insertDts = LocalDateTime.now()))
            )

            println("초대 성공: ${sender}님이 ${recipient}님을 초대했습니다. DB에 멤버십 추가 완료.")

        } else { // 이미 방의 멤버인 경우
            // 초대자에게 이미 멤버라고 알려줌
            messagingTemplate.convertAndSendToUser(
                sender, "/queue/errors", "${recipient}님은 이미 방의 멤버입니다."
            )
            println("초대 실패: ${recipient}님은 이미 방의 멤버입니다.")
        }
    }

    @MessageMapping("/chat.kickUser")
    fun kickUser(@Payload chatMessageDto: ChatMessageDto) {
        val sender = chatMessageDto.sender // 강퇴하는 사람
        val recipient = chatMessageDto.recipient // 강퇴당하는 사람
        val roomId = chatMessageDto.roomId

        if (!chatRoomService.isRoomLeader(roomId, sender)) {
            messagingTemplate.convertAndSendToUser(
                sender, "/queue/errors", "당신은 ${roomId} 방장이 아닙니다."
            )
            return
        }
        if (!chatRoomService.doesRoomExist(roomId)) {
             messagingTemplate.convertAndSendToUser(
                sender, "/queue/errors", "채팅방 ${roomId}는 존재하지 않습니다.")
             return
        }

        // 방장 자신은 강퇴로도 방을 나갈 수 없다
        if (recipient == sender) return
        val removed = chatRoomService.removeParticipant(roomId, recipient!!) // DB에서 멤버십 삭제

        if (removed) {
            // 강퇴당하는 사람에게 직접 알림
            messagingTemplate.convertAndSendToUser(
                recipient,
                "/queue/errors",
                "당신은 ${roomId} 방에서 강퇴당했습니다."
            )
            // 모든 방 참가자에게 누가 누구를 강퇴했는지 알림
            messagingTemplate.convertAndSend("/topic/chat/$roomId",
                chatMessageService.withNames(ChatMessageDto(null, MessageType.KICK, roomId, sender, recipient = recipient, insertDts = LocalDateTime.now()))
            )
        } else {
             messagingTemplate.convertAndSendToUser(
                sender, "/queue/errors", "${recipient}는 ${roomId} 방의 멤버가 아닙니다.")
        }
    }

    @MessageMapping("/chat.leaveUser")
    fun leaveUser(@Payload chatMessageDto: ChatMessageDto) {
        val userId = chatMessageDto.sender
        val roomId = chatMessageDto.roomId

        if (leaderCannotLeave(roomId, userId)) {
            println("방장 나가기 거절 - 방: $roomId, 유저: $userId")
            return
        }
        val removed = chatRoomService.removeParticipant(roomId, userId)

        if (removed) {
            println("유저 퇴장 - 방: $roomId, 유저: $userId")
            messagingTemplate.convertAndSend("/topic/chat/$roomId",
                chatMessageService.withNames(ChatMessageDto(null, MessageType.LEAVE, roomId, userId, "${userId}님이 퇴장했습니다.", insertDts = LocalDateTime.now()))
            )
        } else {
            println("유저 ${userId}가 방 ${roomId}에서 나가지 못했습니다. (존재하지 않거나 이미 나감)")
            messagingTemplate.convertAndSendToUser(
                userId, "/queue/errors", "채팅방 ${roomId}에서 나갈 수 없습니다. (이미 나갔거나 방이 없습니다.)")
        }
    }
}
