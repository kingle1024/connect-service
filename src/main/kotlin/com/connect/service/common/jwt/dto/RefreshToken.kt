package com.connect.service.common.jwt.dto

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "refresh_tokens")
data class RefreshToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    val userId: String, // User 엔티티의 userId(String)와 매핑

    @Column(name = "refresh_token", nullable = false, length = 500, unique = true)
    val token: String, // 실제 Refresh Token 값

    @Column(name = "issued_at", nullable = false)
    val issuedAt: LocalDateTime,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime,

    @Column(name = "revoked_at")
    var revokedAt: LocalDateTime? = null, // 무효화된 시각

    @Column(name = "is_revoked", nullable = false)
    var isRevoked: Boolean = false, // 무효화 여부

    @Column(name = "ip_address", length = 45)
    val ipAddress: String? = null,

    @Column(name = "user_agent", length = 255)
    val userAgent: String? = null
)
