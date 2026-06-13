package com.connect.service.reminder.scheduler

import com.connect.service.reminder.service.ReminderService
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class ReminderScheduler(
    private val reminderService: ReminderService
) {
    companion object {
        private val log = KotlinLogging.logger {}
    }

    // 매일 오전 9시에 당일 알림을 이메일로 발송
    @Scheduled(cron = "0 0 9 * * *")
    fun sendTodayReminders() {
        val today = LocalDate.now()
        val sent = reminderService.sendDueReminders(today)
        if (sent > 0) {
            log.info("일정 알림 이메일 발송 완료: {}건 (날짜={})", sent, today)
        }
    }
}
