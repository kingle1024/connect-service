package com.connect.service.friend.controller

import com.connect.service.chatting.dto.CreateOneToOneChatRequest
import com.connect.service.chatting.dto.SendMessageRequest
import com.connect.service.friend.dto.FriendRequestProcessDto
import com.connect.service.friend.dto.FriendRequestSendDto
import com.connect.service.friend.dto.FriendResponse
import com.connect.service.friend.service.FriendService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/friends/{currentUserId}")
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:8082"], allowCredentials = "true")
class FriendController(
    private val friendService: FriendService
) {
    // --- 친구 요청 기능 ---

    @PostMapping("/friend-requests")
    fun sendFriendRequest(@PathVariable currentUserId: String, @RequestBody requestDto: FriendRequestSendDto): ResponseEntity<Any> {
        return try {
            val friendRequest = friendService.sendFriendRequest(currentUserId, requestDto)
            ResponseEntity.status(HttpStatus.CREATED).body(mapOf("message" to "친구 요청을 보냈습니다.", "requestId" to friendRequest.id))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("message" to e.message))
        }
    }

    @PutMapping("/friend-requests/{requestId}/process")
    fun processFriendRequest(
        @PathVariable currentUserId: String,
        @PathVariable requestId: String,
        @RequestBody processDto: FriendRequestProcessDto
    ): ResponseEntity<Any> {
        return try {
            val friendRequest = friendService.processFriendRequest(currentUserId, requestId, processDto)
            ResponseEntity.ok().body(mapOf("message" to "친구 요청을 ${processDto.status} 처리했습니다.", "requestId" to friendRequest.id, "status" to friendRequest.status.name))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @GetMapping("/friend-requests/received")
    fun getReceivedFriendRequests(@PathVariable currentUserId: String): ResponseEntity<List<Any>> {
        val requests = friendService.getReceivedFriendRequests(currentUserId)
        return ResponseEntity.ok(requests)
    }

    // --- 친구 삭제 기능 ---

    @DeleteMapping("/friends/{friendId}")
    fun deleteFriend(@PathVariable currentUserId: String, @PathVariable friendId: String): ResponseEntity<Any> {
        return try {
            friendService.deleteFriend(currentUserId, friendId)
            ResponseEntity.ok().body(mapOf("message" to "친구를 삭제했습니다."))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    // --- 친구 목록 조회 ---

    @GetMapping("/friends")
    fun getFriends(@PathVariable currentUserId: String): ResponseEntity<List<FriendResponse>> {
        val friends = friendService.getFriends(currentUserId)
        return ResponseEntity.ok(friends)
    }

    // --- 대화하기 기능 (Controller 예시) ---
    // 실제로는 ChatController를 따로 만들거나, 기능을 더 세분화해야 합니다.

    @PostMapping("/chats/one-to-one")
    fun createOneToOneChat(@PathVariable currentUserId: String, @RequestBody request: CreateOneToOneChatRequest): ResponseEntity<Any> {
        return try {
            val roomId = friendService.createOneToOneChatRoom(currentUserId, request.participantUserId)
            ResponseEntity.status(HttpStatus.CREATED).body(mapOf("message" to "1:1 채팅방을 생성했습니다.", "roomId" to roomId))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @GetMapping("/chats")
    fun getChatRooms(@PathVariable currentUserId: String): ResponseEntity<List<String>> {
        val chatRooms = friendService.getChatRooms(currentUserId)
        return ResponseEntity.ok(chatRooms)
    }

    @PostMapping("/chats/{roomId}/messages")
    fun sendMessage(@PathVariable currentUserId: String, @PathVariable roomId: Long, @RequestBody message: SendMessageRequest): ResponseEntity<Any> {
        return try {
            val result = friendService.sendMessage(currentUserId, roomId, message.content)
            ResponseEntity.ok().body(mapOf("message" to result))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }
}
