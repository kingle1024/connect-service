package com.connect.service.friend.repository

import com.connect.service.friend.entity.FriendRequest
import com.connect.service.friend.enum.FriendRequestStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FriendRequestRepository : JpaRepository<FriendRequest, Long> {
    fun findBySenderIdAndReceiverId(senderId: String, receiverId: String): FriendRequest?
    fun findByReceiverIdAndStatus(receiverId: String, status: FriendRequestStatus): List<FriendRequest>
}
