package com.connect.service.chatting.controller

import com.connect.service.chatting.dto.ChatMessageDto
import com.connect.service.chatting.dto.ChatRoomDto
import com.connect.service.chatting.dto.CreateChatRoomRequest
import com.connect.service.chatting.service.ChatMessageService
import com.connect.service.chatting.service.ChatRoomService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:8082"], allowCredentials = "true")
class ChatRestController(
    private val chatRoomService: ChatRoomService,
    private val chatMessageService: ChatMessageService
) {

    @PostMapping("/rooms")
    fun createChatRoom(
        @RequestBody request: CreateChatRoomRequest
    ): String {
        var added = false

        if (request.userId.contains("|")) {
            request.userId.split("|").forEach { uid ->
                added = chatRoomService.addParticipant(request.roomId, uid, request.roomName)
            }
        } else {
            added = chatRoomService.addParticipant(request.roomId, request.userId, request.roomName)
        }

        return if (added) {
            "채팅방 $request.roomId 에 사용자 $request.userId 가 성공적으로 추가되었습니다."
        } else {
            "사용자 $request.userId 는 이미 채팅방 $request.roomId 의 멤버입니다."
        }
    }

    @GetMapping("/rooms")
    fun getChatRoomsForUser(@RequestParam userId: String): List<ChatRoomDto> {
        println("채팅방 목록 요청 - 사용자 ID: $userId")
        return chatRoomService.getRoomsForUser(userId)
    }

    @GetMapping("/one-to-one-rooms")
    fun oneToOneRooms(@RequestParam userId: String): List<ChatRoomDto> {
        println("1:1 채팅방 목록 요청 - 사용자 ID: $userId")
        return chatRoomService.getOneToOneRoomsForUser(userId)
    }

    @GetMapping("/rooms/{roomId}/messages")
    fun getRoomMessages(@PathVariable roomId: String): List<ChatMessageDto> {
        return chatMessageService.getChatHistoryForRoom(roomId)
             .takeLast(50)
    }
}
