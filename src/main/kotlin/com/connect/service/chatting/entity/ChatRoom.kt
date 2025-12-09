package com.connect.service.chatting.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity // 이 클래스가 JPA 엔티티임을 나타냄
@Table(name = "chat_rooms") // 매핑될 테이블 이름
data class ChatRoom(
    @Id // 기본 키
    @Column(name = "room_id", unique = true, nullable = false) // 컬럼 이름과 제약조건
    val roomId: String, // 채팅방 고유 ID

    @Column(name = "room_name", nullable = false)
    var roomName: String, // 채팅방 이름

    @Column(name = "roomType", nullable = false)
    var roomType: String, // 채팅방 유형

    @Column(name = "leader_user_id", nullable = false)
    var leaderUserId: String, // 방장 ID

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now() // 생성 시간
)
