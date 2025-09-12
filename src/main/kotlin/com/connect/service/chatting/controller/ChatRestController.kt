package com.connect.service.chatting.controller

import com.connect.service.chatting.service.ChatRoomDto
import com.connect.service.chatting.service.ChatRoomService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin
class ChatRestController(
    private val chatRoomService: ChatRoomService // 채팅방 관리 서비스
) {

    @GetMapping("/api/chat/rooms")
    fun getChatRoomsForUser(@RequestParam userId: String): List<ChatRoomDto> {
        println("채팅방 목록 요청 - 사용자 ID: $userId")
        return chatRoomService.getRoomsForUser(userId)
    }
}
