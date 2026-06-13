package com.connect.service.reminder.service

import com.connect.service.reminder.dto.ReminderCreateRequest
import com.connect.service.reminder.entity.DateReminder
import com.connect.service.reminder.repository.ReminderRepository
import com.connect.service.user.domain.UserRole
import com.connect.service.user.repository.UserRepository
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate

@Service
class ReminderService(
    private val reminderRepository: ReminderRepository,
    private val userRepository: UserRepository,
    private val javaMailSender: JavaMailSender
) {
    companion object {
        private val log = KotlinLogging.logger {}
    }

    // 알림 등록. email 미지정 시 계정 이메일 사용.
    @Transactional
    fun create(userId: String, request: ReminderCreateRequest): DateReminder {
        if (request.content.isBlank()) {
            throw IllegalArgumentException("알림 내용을 입력해주세요.")
        }
        // 알림 등록은 누구나 가능(마이페이지 조회용). 단, 이메일 발송은 인증 사용자만(sendDueReminders 참고)
        val user = userRepository.findByUserId(userId)
            ?: throw IllegalArgumentException("사용자를 찾을 수 없습니다.")
        val email = request.email?.trim()?.takeIf { it.isNotEmpty() } ?: user.email

        val reminder = DateReminder(
            userId = userId,
            email = email,
            reminderDate = request.reminderDate,
            content = request.content.trim()
        )
        return reminderRepository.save(reminder)
    }

    @Transactional(readOnly = true)
    fun list(userId: String): List<DateReminder> {
        return reminderRepository.findByUserIdOrderByReminderDateAsc(userId)
    }

    @Transactional
    fun delete(userId: String, id: Long) {
        val reminder = reminderRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다.") }
        if (reminder.userId != userId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "본인 알림만 삭제할 수 있습니다.")
        }
        reminderRepository.delete(reminder)
    }

    // 해당 날짜의 미발송 알림을 이메일로 발송하고 발송 완료로 표시. (스케줄러에서 호출)
    // ★이메일 발송은 더존 이메일 인증(ROLE_VERIFIED) 사용자만★ (미인증 사용자의 알림은 마이페이지 조회 전용)
    @Transactional
    fun sendDueReminders(today: LocalDate): Int {
        val dueList = reminderRepository.findByReminderDateAndNotifiedFalse(today)
        if (dueList.isEmpty()) {
            return 0
        }
        // 인증 완료 사용자 ID 집합 (이 사용자들의 알림만 메일 발송)
        val verifiedUserIds = userRepository.findByUserIdIn(dueList.map { it.userId }.distinct())
            .filter { it.roles.contains(UserRole.ROLE_VERIFIED) }
            .map { it.userId }
            .toSet()

        dueList.filter { it.userId in verifiedUserIds }.forEach { reminder ->
            try {
                val message = SimpleMailMessage()
                message.setTo(reminder.email)
                message.setSubject("[같이타] 일정 알림")
                message.setText("오늘(${reminder.reminderDate}) 일정 알림입니다.\n\n- ${reminder.content}")
                javaMailSender.send(message)
                reminder.notified = true
            } catch (e: Exception) {
                // 한 건 실패가 전체를 막지 않도록 로깅 후 계속 (notified 는 false 유지 → 다음 회차 재시도)
                log.error("알림 이메일 발송 실패: id=${reminder.id}, email=${reminder.email}, 이유=${e.message}")
            }
        }
        reminderRepository.saveAll(dueList)
        return dueList.count { it.notified }
    }
}
