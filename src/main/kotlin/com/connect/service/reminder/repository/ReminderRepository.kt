package com.connect.service.reminder.repository

import com.connect.service.reminder.entity.DateReminder
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface ReminderRepository : JpaRepository<DateReminder, Long> {
    // 마이페이지: 내 알림 목록 (날짜 오름차순)
    fun findByUserIdOrderByReminderDateAsc(userId: String): List<DateReminder>

    // 스케줄러: 해당 날짜에 아직 발송하지 않은 알림
    fun findByReminderDateAndNotifiedFalse(reminderDate: LocalDate): List<DateReminder>
}
