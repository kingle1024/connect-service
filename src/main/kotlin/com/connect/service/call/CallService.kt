package com.connect.service.call

import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

/**
 * 채팅방 음성 통화에 지금 들어와 있는 사람을 기억한다 (서버 메모리).
 *
 * 통화 음성은 브라우저끼리 WebRTC 로 직접 주고받고, 서버는 누가 들어왔는지와
 * 연결 정보(offer/answer/ice)만 전달한다. 서버가 재시작되면 진행 중인 통화 목록은 비워진다.
 */
@Service
class CallService {
    // roomId -> (userId -> 웹소켓 세션 ID)
    private val calls = ConcurrentHashMap<String, MutableMap<String, String>>()

    fun participants(roomId: String): List<String> = calls[roomId]?.keys?.toList() ?: emptyList()

    /** 통화에 들어온다. 이 사람이 첫 참가자(=통화 시작)면 true */
    fun join(roomId: String, userId: String, sessionId: String): Boolean {
        val room = calls.computeIfAbsent(roomId) { ConcurrentHashMap() }
        synchronized(room) {
            val started = room.isEmpty()
            room[userId] = sessionId
            return started
        }
    }

    /** 통화에서 나간다. 실제로 나간 경우 true */
    fun leave(roomId: String, userId: String): Boolean {
        val room = calls[roomId] ?: return false
        synchronized(room) {
            val removed = room.remove(userId) != null
            if (room.isEmpty()) calls.remove(roomId, room)
            return removed
        }
    }

    /** 웹소켓이 끊긴 세션이 들어가 있던 통화에서 모두 뺀다. (roomId, userId) 목록을 돌려준다 */
    fun leaveBySession(sessionId: String): List<Pair<String, String>> {
        val left = mutableListOf<Pair<String, String>>()
        calls.forEach { (roomId, room) ->
            room.filterValues { it == sessionId }.keys.forEach { userId ->
                if (leave(roomId, userId)) left += roomId to userId
            }
        }
        return left
    }

    companion object {
        // 브라우저끼리 모두 직접 연결(mesh)하므로 인원이 많으면 끊긴다
        const val MAX_PARTICIPANTS = 8
    }
}
