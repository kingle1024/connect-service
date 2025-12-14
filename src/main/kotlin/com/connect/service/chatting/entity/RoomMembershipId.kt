package com.connect.service.chatting.entity

import jakarta.persistence.*
import java.io.Serializable // 복합 키를 위해 필요
import java.time.LocalDateTime

@Embeddable // 다른 엔티티에 포함될 수 있는 임베디드 타입
data class RoomMembershipId(
    @Column(name = "user_id")
    var userId: String, // 사용자 ID

    @Column(name = "room_id")
    var roomId: String // 채팅방 ID
) : Serializable // Serializable을 구현해야 함

@Entity
@Table(name = "room_memberships") // 사용자와 방의 관계를 저장하는 테이블
data class RoomMembership(
    @EmbeddedId // 복합 기본 키
    var id: RoomMembershipId, // 위에서 정의한 복합 키 객체

    @ManyToOne // RoomMembership(다) : ChatRoom(일)
    @MapsId("roomId") // RoomMembershipId의 roomId 필드에 매핑
    @JoinColumn(name = "room_id", referencedColumnName = "room_id") // 실제 DB 컬럼 매핑
    val chatRoom: ChatRoom, // 이 멤버십이 속한 채팅방 엔티티

    @Column(name = "room_name")
    var roomName: String?,

    @Column(name = "joined_at", nullable = false)
    val joinedAt: LocalDateTime = LocalDateTime.now() // 참여 시간
)
