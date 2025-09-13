package com.connect.service.chatting.entity

import com.connect.service.chatting.enums.MessageType
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "chat_messages") // 매핑될 DB 테이블 이름
data class ChatMessages(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // ID 자동 생성 (DB에 위임)
    val id: Long = 0, // 기본값을 0으로 설정하면 새 엔티티 저장 시 JPA가 ID를 할당

    @Enumerated(EnumType.STRING) // Enum 값을 DB에 문자열로 저장
    @Column(name = "message_type", nullable = false)
    val type: MessageType,

    @Column(name = "room_id", nullable = false)
    val roomId: String,

    @Column(name = "sender", nullable = false)
    val sender: String,

    @Column(name = "content") // null을 허용 (JOIN/LEAVE 메시지 등)
    val content: String? = null,

    @Column(name = "recipient") // null을 허용 (INVITE/KICK 메시지 등)
    val recipient: String? = null,

    @Column(name = "timestamp", nullable = false)
    val timestamp: LocalDateTime = LocalDateTime.now() // 메시지 생성 시간 자동 기록
)
