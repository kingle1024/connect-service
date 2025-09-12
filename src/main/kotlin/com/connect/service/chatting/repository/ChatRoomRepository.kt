package com.connect.service.chatting.repository

import com.connect.service.chatting.entity.ChatRoom
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository // 스프링의 리포지토리 컴포넌트임을 나타냄
interface ChatRoomRepository : JpaRepository<ChatRoom, String> {
    // JpaRepository를 상속하면 기본적인 CRUD(생성, 조회, 수정, 삭제) 메서드들을 자동으로 제공받아.
    // ChatRoom 엔티티를 관리하고, 기본 키 타입은 String
}
