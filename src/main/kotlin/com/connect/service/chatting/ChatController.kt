package com.connect.service.chatting

import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

@Controller
class ChatController(
    private val messagingTemplate: SimpMessagingTemplate, // 메시지 보내는 데 사용해
    private val chatRoomService: ChatRoomService // 채팅방 관리 서비스
) {

    // 클라이언트가 /app/chat.sendMessage 로 메시지를 보내면 이 메서드가 처리해
    @MessageMapping("/chat.sendMessage")
    fun sendMessage(@Payload chatMessage: ChatMessage) {
        println("메시지 수신 - 방: ${chatMessage.roomId}, 보낸이: ${chatMessage.sender}, 내용: ${chatMessage.content}")
        // 해당 채팅방을 구독하고 있는 모든 클라이언트에게 메시지를 보내
        // 목적지: /topic/chat/{roomId}
        messagingTemplate.convertAndSend("/topic/chat/${chatMessage.roomId}", chatMessage)
    }

    // 클라이언트가 /app/chat.addUser 로 메시지를 보내면 이 메서드가 처리해
    // 새로운 유저가 채팅방에 들어왔을 때 사용돼
    @MessageMapping("/chat.addUser")
    fun addUser(@Payload chatMessage: ChatMessage, headerAccessor: SimpMessageHeaderAccessor) {
        val userId = chatMessage.sender // 메시지 보낸 사람 = 입장하는 유저
        val roomId = chatMessage.roomId

        println("유저 입장 - 방: $roomId, 유저: $userId")

        // 채팅방에 유저 추가하고 세션 정보 저장
        if (chatRoomService.addParticipant(roomId, userId)) {
            // WebSocket 세션에 roomId 저장 (나중에 유저 나갈 때 활용하려고)
            headerAccessor.sessionAttributes?.put("roomId", roomId)
            headerAccessor.sessionAttributes?.put("username", userId)

            // 채팅방 모든 멤버에게 입장 메시지 발송
            messagingTemplate.convertAndSend("/topic/chat/$roomId",
                ChatMessage(MessageType.JOIN, roomId, userId, "${userId}님이 입장했습니다!"))
        } else {
            // 채팅방이 없거나 이미 참여 중인 경우 (오류 처리 필요)
            println("유저 ${userId}가 방 ${roomId}에 참여할 수 없어. 이미 있거나 방이 없어.")
            // 특정 유저에게만 메시지 보내기 (ex: 오류 메시지)
            messagingTemplate.convertAndSendToUser(userId, "/queue/errors",
                "방에 입장할 수 없습니다.")
        }
    }

    // **방장 기능: 유저 초대**
    // /app/chat.inviteUser 로 메시지를 보내면 처리
    @MessageMapping("/chat.inviteUser")
    fun inviteUser(@Payload chatMessage: ChatMessage) {
        val inviter = chatMessage.sender // 메시지 보낸 사람 (초대하는 사람)
        val invitee = chatMessage.recipient // 초대받는 사람!
        val roomId = chatMessage.roomId

        if (invitee == null) {
            println("초대할 사람이 지정되지 않았어.")
            return
        }

        // 방장이 맞는지 확인해야 해!
        if (!chatRoomService.isRoomLeader(roomId, inviter)) {
            println("${inviter}님은 방장이 아니라서 ${invitee}님을 초대할 수 없어.")
            // 방장만 가능하다고 방장에게 메시지 보내기
            messagingTemplate.convertAndSendToUser(inviter, "/queue/errors",
                "초대는 방장만 할 수 있습니다.")
            return
        }

        // 실제로 유저를 채팅방에 추가해
        if (chatRoomService.addParticipant(roomId, invitee)) {
            println("${inviter}님이 방 ${roomId}에 ${invitee}님을 초대했어!")
            // 모든 채팅방 멤버에게 초대 메시지 발송
            messagingTemplate.convertAndSend("/topic/chat/$roomId",
                ChatMessage(MessageType.INVITE, roomId, inviter, "${inviter}님이 ${invitee}님을 초대했습니다!", invitee))
            // 초대받은 유저에게도 개인 메시지로 알림 (optional)
            messagingTemplate.convertAndSendToUser(invitee, "/queue/invitations",
                "방 ${roomId}에 초대되셨습니다!")
        } else {
            println("${invitee}님은 이미 방 ${roomId}에 있거나, 초대 실패.")
            messagingTemplate.convertAndSendToUser(inviter, "/queue/errors",
                "${invitee}님을 초대하지 못했습니다. 이미 참여 중이거나 문제가 발생했습니다.")
        }
    }

    // **방장 기능: 유저 강퇴**
    // /app/chat.kickUser 로 메시지를 보내면 처리
    @MessageMapping("/chat.kickUser")
    fun kickUser(@Payload chatMessage: ChatMessage) {
        val kicker = chatMessage.sender // 메시지 보낸 사람 (강퇴하는 사람)
        val kickedUser = chatMessage.recipient // 강퇴당할 사람!
        val roomId = chatMessage.roomId

        if (kickedUser == null) {
            println("강퇴할 사람이 지정되지 않았어.")
            return
        }

        // 방장이 맞는지 확인해야 해!
        if (!chatRoomService.isRoomLeader(roomId, kicker)) {
            println("${kicker}님은 방장이 아니라서 ${kickedUser}님을 강퇴할 수 없어.")
            // 방장만 가능하다고 방장에게 메시지 보내기
            messagingTemplate.convertAndSendToUser(kicker, "/queue/errors",
                "강퇴는 방장만 할 수 있습니다.")
            return
        }

        // 방장 자신은 강퇴할 수 없어
        if (kicker == kickedUser) {
            println("${kicker}님은 자신을 강퇴할 수 없어.")
            messagingTemplate.convertAndSendToUser(kicker, "/queue/errors",
                "자신을 강퇴할 수 없습니다.")
            return
        }

        // 실제로 유저를 채팅방에서 제거해
        if (chatRoomService.removeParticipant(roomId, kickedUser)) {
            println("${kicker}님이 방 ${roomId}에서 ${kickedUser}님을 강퇴했어!")
            // 모든 채팅방 멤버에게 강퇴 메시지 발송
            messagingTemplate.convertAndSend("/topic/chat/$roomId",
                ChatMessage(MessageType.KICK, roomId, kicker, "${kicker}님이 ${kickedUser}님을 강퇴했습니다!", kickedUser))
            // 강퇴당한 유저에게도 개인 메시지로 알림 (필수적)
            messagingTemplate.convertAndSendToUser(kickedUser, "/queue/kicked",
                "방 ${roomId}에서 강퇴당하셨습니다.")
            // 강퇴당한 유저의 웹소켓 연결 끊기 (실제로는 세션 종료 로직이 필요)
            // 여기서는 SimpMessagingTemplate으로 특정 세션 종료시키는 기능은 없어. 별도 로직 필요.
        } else {
            println("${kickedUser}님은 방 ${roomId}에 없거나, 강퇴 실패.")
            messagingTemplate.convertAndSendToUser(kicker, "/queue/errors",
                "${kickedUser}님을 강퇴하지 못했습니다. 해당 유저가 없거나 문제가 발생했습니다.")
        }
    }
}
