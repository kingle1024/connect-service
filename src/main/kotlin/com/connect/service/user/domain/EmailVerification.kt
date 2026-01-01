package com.connect.service.user.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "email_verification")
data class EmailVerification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MariaDB의 AUTO_INCREMENT에 매핑
    val id: Long = 0,

    @Column(nullable = false)
    val email: String,

    @Column(name = "verification_code", nullable = false, length = 10)
    val verificationCode: String,

    @Column(name = "created_at", nullable = false, updatable = false) // 생성 시간, 업데이트 불가
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime,

    @Column(name = "is_used", nullable = false)
    var isUsed: Boolean = false // var로 선언하여 상태 변경 가능하도록
)
