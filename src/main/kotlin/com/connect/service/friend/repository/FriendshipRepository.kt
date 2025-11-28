package com.connect.service.friend.repository

import com.connect.service.friend.entity.Friendship
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FriendshipRepository : JpaRepository<Friendship, Long> {
    fun findByUserId(userId: String): List<Friendship>
    fun findByUserIdAndFriendId(userId: String, friendId: String): Friendship?
    @Transactional
    fun deleteByUserIdAndFriendId(userId: String, friendId: String)
}
