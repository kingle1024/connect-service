package com.connect.service.chatting.controller

import com.connect.service.chatting.dto.ChatMessageDto
import com.connect.service.chatting.service.ChatMessageService
import com.connect.service.chatting.service.ChatRoomDto
import com.connect.service.chatting.service.ChatRoomService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/chat")
@CrossOrigin
class ChatRestController(
    private val chatRoomService: ChatRoomService,
    private val chatMessageService: ChatMessageService
) {

    @GetMapping("/rooms")
    fun getChatRoomsForUser(@RequestParam userId: String): List<ChatRoomDto> {
        println("채팅방 목록 요청 - 사용자 ID: $userId")
        return chatRoomService.getRoomsForUser(userId)
    }

    @GetMapping("/rooms/{roomId}/messages")
    fun getRoomMessages(@PathVariable roomId: String): List<ChatMessageDto> {
        return chatMessageService.getChatHistoryForRoom(roomId)
             .takeLast(50)
    }
}
