package com.connect.service.friend.entity

import com.connect.service.friend.enum.FriendRequestStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(name = "friend_requests",
    uniqueConstraints = [UniqueConstraint(columnNames = ["sender_id", "receiver_id"])]
)
data class FriendRequest(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(name = "sender_id", nullable = false, length = 100) // users.user_id (VARCHAR(100)) 참조
    val senderId: String,

    @Column(name = "receiver_id", nullable = false, length = 100) // users.user_id (VARCHAR(100)) 참조
    val receiverId: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: FriendRequestStatus = FriendRequestStatus.PENDING,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
