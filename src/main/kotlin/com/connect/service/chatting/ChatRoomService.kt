package com.connect.service.chatting

import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class ChatRoomService {
    // roomId를 키로 ChatRoom 객체를 저장할 맵
    private val chatRooms: ConcurrentHashMap<String, ChatRoom> = ConcurrentHashMap()

    // 채팅방을 새로 만들고, 만든 사람을 방장으로 등록해
    fun createChatRoom(roomId: String, roomLeader: String): ChatRoom {
        val newRoom = ChatRoom(roomId, roomLeader)
        chatRooms[roomId] = newRoom
        // 방장을 바로 참여자 목록에 추가해줘
        newRoom.addParticipant(roomLeader)
        println("채팅방 '$roomId'가 생성되었고, 방장은 '$roomLeader'님이야!")
        return newRoom
    }

    // 채팅방 찾기
    fun findRoomById(roomId: String): ChatRoom? {
        return chatRooms[roomId]
    }

    // 채팅방에 유저를 추가하는 기능 (초대 기능에 쓰이겠지?)
    fun addParticipant(roomId: String, userId: String): Boolean {
        val room = findRoomById(roomId) ?: return false // 방이 없으면 실패
        return room.addParticipant(userId)
    }

    // 채팅방에서 유저를 제거하는 기능 (강퇴 기능에 쓰이겠지?)
    fun removeParticipant(roomId: String, userId: String): Boolean {
        val room = findRoomById(roomId) ?: return false // 방이 없으면 실패
        // 방장이 나가려고 하면 특별한 처리 필요 (방장 위임 또는 방 폭파)
        if (room.roomLeader == userId) {
            println("경고: 방장은 스스로 강퇴될 수 없어. 방을 닫거나 다른 방장을 지정해야 해.")
            return false
        }
        return room.removeParticipant(userId)
    }

    // 유저가 해당 방의 방장인지 확인하는 기능
    fun isRoomLeader(roomId: String, userId: String): Boolean {
        val room = findRoomById(roomId) ?: return false
        return room.roomLeader == userId
    }

    // 현재 채팅방 목록 확인 (테스트용)
    fun getAllChatRooms(): List<ChatRoom> {
        return chatRooms.values.toList()
    }
}
