package com.connect.service.friend.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "friendships",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "friend_id"])]
)
data class Friendship(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(name = "user_id", nullable = false, length = 100) // users.user_id (VARCHAR(100)) 참조
    val userId: String,

    @Column(name = "friend_id", nullable = false, length = 100) // users.user_id (VARCHAR(100)) 참조
    val friendId: String,

    @Column(name = "is_blocked", nullable = false)
    val isBlocked: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
