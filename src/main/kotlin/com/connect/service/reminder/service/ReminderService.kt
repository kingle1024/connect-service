package com.connect.service.reminder.service

import com.connect.service.reminder.dto.ReminderCreateRequest
import com.connect.service.reminder.entity.DateReminder
import com.connect.service.reminder.repository.ReminderRepository
import com.connect.service.user.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

// 알림 등록/조회/삭제 담당. 이메일 발송(스케줄러)은 별도 배치(connect-batch)로 분리됨.
@Service
class ReminderService(
    private val reminderRepository: ReminderRepository,
    private val userRepository: UserRepository
) {

    // 알림 등록. 발송 대상 이메일은 항상 계정(인증) 이메일을 사용 - 별도 입력 받지 않음.
    // 등록은 누구나 가능(이메일 발송만 인증 사용자 한정 - 배치에서 처리)
    @Transactional
    fun create(userId: String, request: ReminderCreateRequest): DateReminder {
        if (request.content.isBlank()) {
            throw IllegalArgumentException("알림 내용을 입력해주세요.")
        }
        val user = userRepository.findByUserId(userId)
            ?: throw IllegalArgumentException("사용자를 찾을 수 없습니다.")

        val reminder = DateReminder(
            userId = userId,
            email = user.email, // 계정(인증) 이메일로 발송
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
}
