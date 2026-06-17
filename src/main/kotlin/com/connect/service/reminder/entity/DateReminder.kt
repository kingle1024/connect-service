package com.connect.service.reminder.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

// 특정 날짜 알림. 해당 날짜가 되면 이메일로 알림을 발송한다.
@Entity
@Table(name = "date_reminder")
data class DateReminder(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    val userId: String, // 알림을 등록한 사용자

    @Column(nullable = false)
    val email: String, // 알림을 받을 이메일

    @Column(name = "reminder_date", nullable = false)
    val reminderDate: LocalDate, // 알림 날짜 (예: 2026-09-12)

    @Column(nullable = false)
    val content: String, // 알림 내용 (예: 오전 반차)

    @Column(nullable = false)
    var notified: Boolean = false, // 발송 완료 여부 (중복 발송 방지)

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
