package com.connect.service.call

import com.connect.service.chatting.repository.RoomMembershipRepository
import com.connect.service.user.repository.UserRepository
import org.springframework.context.event.EventListener
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.socket.messaging.SessionDisconnectEvent

/**
 * 채팅방 음성 통화(2명이면 1:1, 여러 명이면 그룹 통화) 신호 중계.
 *
 * 앱 → /app/call.signal 로 보내면 /topic/call/{roomId} 로 방 사람들에게 뿌린다.
 *  - join / leave : 통화 참가·퇴장. 서버가 참가자 목록(participants)을 붙여서 보낸다
 *  - offer / answer / ice : WebRTC 연결 정보. to 에 받을 사람을 적고, 받는 쪽이 자기 것만 골라 쓴다
 */
@Controller
class CallSignalController(
    private val messagingTemplate: SimpMessagingTemplate,
    private val callService: CallService,
    private val roomMembershipRepository: RoomMembershipRepository,
    private val userRepository: UserRepository,
) {
    data class CallSignal(
        val type: String = "",
        val roomId: String = "",
        val from: String = "",
        val to: String? = null,
        val sdp: Any? = null,
        val candidate: Any? = null,
        val participants: List<String>? = null,
        // participants 의 표시 이름 (게스트는 "용감한 코알라 12" 같은 닉네임)
        val names: Map<String, String>? = null,
        val started: Boolean? = null,
        val error: String? = null,
    )

    @MessageMapping("/call.signal")
    fun signal(@Payload signal: CallSignal, headerAccessor: SimpMessageHeaderAccessor) {
        val roomId = signal.roomId
        val from = signal.from
        if (roomId.isBlank() || from.isBlank()) return
        // 방 멤버만 통화할 수 있다
        if (!roomMembershipRepository.existsByIdUserIdAndIdRoomId(from, roomId)) return

        when (signal.type) {
            "join" -> {
                val current = callService.participants(roomId)
                if (from !in current && current.size >= CallService.MAX_PARTICIPANTS) {
                    send(roomId, CallSignal(type = "full", roomId = roomId, from = from, to = from,
                        error = "통화 인원(${CallService.MAX_PARTICIPANTS}명)이 가득 찼어요."))
                    return
                }
                val started = callService.join(roomId, from, headerAccessor.sessionId ?: "")
                send(roomId, stateSignal("join", roomId, from).copy(started = started))
            }
            "leave" -> {
                if (callService.leave(roomId, from)) {
                    send(roomId, stateSignal("leave", roomId, from))
                }
            }
            "offer", "answer", "ice" -> {
                if (signal.to.isNullOrBlank()) return
                send(roomId, signal.copy(participants = null, names = null, started = null, error = null))
            }
        }
    }

    // 브라우저를 닫거나 네트워크가 끊기면 통화에서 뺀다
    @EventListener
    fun onDisconnect(event: SessionDisconnectEvent) {
        callService.leaveBySession(event.sessionId).forEach { (roomId, userId) ->
            send(roomId, stateSignal("leave", roomId, userId))
        }
    }

    // 채팅방에 들어왔을 때 진행 중인 통화가 있는지 확인용
    @GetMapping("/api/chat/rooms/{roomId}/call")
    @ResponseBody
    fun current(@PathVariable roomId: String): CallSignal = stateSignal("state", roomId, "")

    // 지금 통화 참가자와 그 표시 이름을 담은 신호
    private fun stateSignal(type: String, roomId: String, from: String): CallSignal {
        val participants = callService.participants(roomId)
        return CallSignal(type = type, roomId = roomId, from = from, participants = participants, names = namesOf(participants))
    }

    private fun namesOf(userIds: List<String>): Map<String, String> =
        if (userIds.isEmpty()) emptyMap()
        else userRepository.findByUserIdIn(userIds).filter { it.name.isNotBlank() }.associate { it.userId to it.name }

    private fun send(roomId: String, signal: CallSignal) =
        messagingTemplate.convertAndSend("/topic/call/$roomId", signal)
}
