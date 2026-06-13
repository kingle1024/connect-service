package com.connect.service.reminder.dto

import com.connect.service.reminder.entity.DateReminder
import java.time.LocalDate

// 알림 등록 요청. email 미지정 시 계정 이메일로 발송.
data class ReminderCreateRequest(
    val reminderDate: LocalDate, // "2026-09-12" 형식 (ISO)
    val content: String,
    val email: String? = null
)

data class ReminderResponse(
    val id: Long,
    val reminderDate: LocalDate,
    val content: String,
    val email: String,
    val notified: Boolean
) {
    companion object {
        fun from(r: DateReminder): ReminderResponse {
            return ReminderResponse(
                id = r.id ?: throw IllegalArgumentException("Reminder ID cannot be null"),
                reminderDate = r.reminderDate,
                content = r.content,
                email = r.email,
                notified = r.notified
            )
        }
    }
}
