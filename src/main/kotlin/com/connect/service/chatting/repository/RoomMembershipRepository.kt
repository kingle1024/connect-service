package com.connect.service.chatting.repository

import com.connect.service.chatting.entity.RoomMembership
import com.connect.service.chatting.entity.RoomMembershipId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RoomMembershipRepository : JpaRepository<RoomMembership, RoomMembershipId> {
    // userId로 모든 멤버십을 조회
    fun findByIdUserId(userId: String): List<RoomMembership>
    fun findByIdRoomId(roomId: String): List<RoomMembership>
    // 특정 방의 멤버십 개수를 세는 쿼리
    fun countByIdRoomId(roomId: String): Long

    // 특정 유저의 특정 방 멤버십 삭제 (복합 키를 통해 직접 조회 후 삭제)
    fun deleteByIdUserIdAndIdRoomId(userId: String, roomId: String)

    // 특정 유저의 특정 방 멤버십 존재 여부 확인
    fun existsByIdUserIdAndIdRoomId(userId: String, roomId: String): Boolean
}
