package com.connect.service.chatting.controller

import com.connect.service.chatting.dto.ChatMessageDto
import com.connect.service.chatting.dto.ChatOneToOneRoomDto
import com.connect.service.chatting.dto.ChatRoomDto
import com.connect.service.chatting.dto.ChatRoomParticipantReq
import com.connect.service.chatting.dto.ChatRoomParticipantRes
import com.connect.service.chatting.dto.CreateChatRoomRequest
import com.connect.service.chatting.dto.RoomNameUpdateRequest
import com.connect.service.chatting.entity.ChatRoom
import com.connect.service.user.dto.CustomUserDto
import com.connect.service.chatting.service.ChatMessageService
import com.connect.service.chatting.service.ChatRoomService
import com.connect.service.user.service.CustomUserDetailsService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:8082"], allowCredentials = "true")
class ChatRestController(
    private val chatRoomService: ChatRoomService,
    private val chatMessageService: ChatMessageService,
    private val customUserService: CustomUserDetailsService,
) {

    @PostMapping("/rooms")
    fun createChatRoom(
        @RequestBody request: CreateChatRoomRequest
    ): CreateChatRoomRequest? {

        val userIds = request.userId.split("|")
        val map: Map<String, CustomUserDto> = customUserService.getRoomMemberShipByUserIds(userIds)
        val participantActions = userIds.map { currentUserId ->
            // userId가 2개인 경우 (1:1 채팅 예상) 상대방 ID를 roomName으로 사용
            val roomNameForParticipant = if (userIds.size == 2) {
                userIds.first { it != currentUserId } // 현재 유저가 아닌 다른 유저 ID를 찾음
            } else {
                request.userId
            }

            val userNameForRoom = map[roomNameForParticipant]?.name ?: "유저"
            Triple(currentUserId, userNameForRoom, request.roomType)
        }

        val allParticipantsAdded = participantActions.all { (currentUserId, roomName, roomType) ->
            chatRoomService.addParticipant(request.roomId, currentUserId, roomName, roomType)
        }

        return if (allParticipantsAdded) {
            request
        } else {
            null
        }
    }

    @GetMapping("/rooms")
    fun getChatRoomsForUser(@RequestParam userId: String): List<ChatRoomDto> {
        println("채팅방 목록 요청 - 사용자 ID: $userId")
        return chatRoomService.getRoomsForUser(userId)
    }

    @PostMapping("/rooms/participants")
    fun getRoomParticipants(@RequestBody request: ChatRoomParticipantReq): List<ChatRoomParticipantRes> {
        println("채팅방 사용자 목록 요청 - ROOM ID: ${request.roomId}")
        return chatRoomService.getRoomsForRoomId(request.roomId)
    }

    @GetMapping("/one-to-one-rooms")
    fun oneToOneRooms(@RequestParam userId: String): List<ChatOneToOneRoomDto> {
        println("1:1 채팅방 목록 요청 - 사용자 ID: $userId")
        return chatRoomService.getOneToOneRoomsForUser(userId)
    }

    @GetMapping("/rooms/{roomId}/messages")
    fun getRoomMessages(@PathVariable roomId: String): List<ChatMessageDto> {
        return chatMessageService.getChatHistoryForRoom(roomId)
             .takeLast(50)
    }

    @PutMapping("/rooms/name")
    fun updateChatRoomName(@RequestBody request: RoomNameUpdateRequest): ResponseEntity<ChatRoom> {
        val updatedChatRoom = chatRoomService.updateRoomName(request.roomId, request.roomName)

        return if (updatedChatRoom != null) {
            ResponseEntity.ok(updatedChatRoom)
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
    }
}
