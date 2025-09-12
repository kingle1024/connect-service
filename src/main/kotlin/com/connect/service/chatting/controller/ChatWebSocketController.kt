package com.connect.service.chatting.controller

import com.connect.service.chatting.dto.ChatMessage
import com.connect.service.chatting.enums.MessageType
import com.connect.service.chatting.service.ChatRoomService
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.web.bind.annotation.CrossOrigin
import kotlin.collections.set

@CrossOrigin
class ChatWebSocketController (
    private val messagingTemplate: SimpMessagingTemplate,
    private val chatRoomService: ChatRoomService
) {
    @MessageMapping("/chat.sendMessage")
    fun sendMessage(@Payload chatMessage: ChatMessage) {
        val roomId = chatMessage.roomId
        val sender = chatMessage.sender
        val content = chatMessage.content

        // 방이 존재하는지 확인 (선택 사항)
        if (!chatRoomService.doesRoomExist(roomId)) {
             messagingTemplate.convertAndSendToUser(
                sender, "/queue/errors", "채팅방 ${roomId}는 존재하지 않습니다.")
             return
        }

        println("메시지 보냄 - 방: $roomId, 발신자: $sender, 내용: $content")
        messagingTemplate.convertAndSend("/topic/chat/$roomId", chatMessage)
    }

    @MessageMapping("/chat.addUser")
    fun addUser(@Payload chatMessage: ChatMessage, headerAccessor: SimpMessageHeaderAccessor) {
        val userId = chatMessage.sender // 메시지 보낸 사람 = 입장하는 유저
        val roomId = chatMessage.roomId
        val roomName = chatMessage.roomName

        headerAccessor.sessionAttributes!!["username"] = userId
        headerAccessor.sessionAttributes!!["roomId"] = roomId
        println("유저 입장 - 방: $roomId, 유저: $userId")

        val added = chatRoomService.addParticipant(roomId, userId, roomName)

        if (added) {
            println("유저 추가됨 - 방: $roomId, 유저: $userId, 방 이름: $roomName")
            messagingTemplate.convertAndSend("/topic/chat/$roomId",
                ChatMessage(MessageType.JOIN, roomId, userId, "${userId}님이 입장했습니다.")
            )
        } else {
            // 이미 방에 있는 경우 or 서비스 로직 상 추가 불가능한 경우 처리
            // 여기서는 이미 존재하는 경우 "이미 입장했습니다." 메시지를 보내도록 예시
            messagingTemplate.convertAndSendToUser(
                userId,
                "/queue/errors",
                "이미 방 ${roomId}에 입장해 있습니다."
            )
        }
    }

    @MessageMapping("/chat.inviteUser")
    fun inviteUser(@Payload chatMessage: ChatMessage) {
        val sender = chatMessage.sender // 초대하는 사람
        val recipient = chatMessage.recipient // 초대받는 사람
        val roomId = chatMessage.roomId
        val roomName = chatMessage.roomName ?: chatRoomService.getRoom(roomId)?.roomName ?: roomId

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

        val addedToRoom = chatRoomService.addParticipant(roomId, recipient!!, roomName)

        if (addedToRoom) { // 성공적으로 추가되었다면 (새로 멤버가 되었다면)
            // 초대받는 사람에게 초대 알림 메시지 발송
            val invitationJson = mapOf(
                "sender" to sender,
                "roomId" to roomId,
                "roomName" to roomName,
                "message" to "${sender}님이 ${roomName} 방으로 당신을 초대했습니다."
            )
            messagingTemplate.convertAndSendToUser(
                recipient, // 받는 사람의 User Queue로 메시지 전송
                "/queue/invitations",
                invitationJson
            )

            // 모든 방 참가자에게 누가 누구를 초대했는지 알림 (채팅방 내 시스템 메시지)
            messagingTemplate.convertAndSend("/topic/chat/$roomId",
                ChatMessage(MessageType.INVITE, roomId, sender, recipient = recipient, roomName = roomName)
            )

            println("초대 성공: ${sender}님이 ${recipient}님을 ${roomName} 방으로 초대했습니다. DB에 멤버십 추가 완료.")

        } else { // 이미 방의 멤버인 경우
            // 초대자에게 이미 멤버라고 알려줌
            messagingTemplate.convertAndSendToUser(
                sender, "/queue/errors", "${recipient}님은 이미 ${roomName} 방의 멤버입니다."
            )
            println("초대 실패: ${recipient}님은 이미 ${roomName} 방의 멤버입니다.")
        }
    }

    @MessageMapping("/chat.kickUser")
    fun kickUser(@Payload chatMessage: ChatMessage) {
        val sender = chatMessage.sender // 강퇴하는 사람
        val recipient = chatMessage.recipient // 강퇴당하는 사람
        val roomId = chatMessage.roomId

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
                ChatMessage(MessageType.KICK, roomId, sender, recipient = recipient)
            )
        } else {
             messagingTemplate.convertAndSendToUser(
                sender, "/queue/errors", "${recipient}는 ${roomId} 방의 멤버가 아닙니다.")
        }
    }

    @MessageMapping("/chat.leaveUser")
    fun leaveUser(@Payload chatMessage: ChatMessage) {
        val userId = chatMessage.sender
        val roomId = chatMessage.roomId

        val removed = chatRoomService.removeParticipant(roomId, userId)

        if (removed) {
            println("유저 퇴장 - 방: $roomId, 유저: $userId")
            messagingTemplate.convertAndSend("/topic/chat/$roomId",
                ChatMessage(MessageType.LEAVE, roomId, userId, "${userId}님이 퇴장했습니다.")
            )
        } else {
            println("유저 ${userId}가 방 ${roomId}에서 나가지 못했습니다. (존재하지 않거나 이미 나감)")
            messagingTemplate.convertAndSendToUser(
                userId, "/queue/errors", "채팅방 ${roomId}에서 나갈 수 없습니다. (이미 나갔거나 방이 없습니다.)")
        }
    }
}
