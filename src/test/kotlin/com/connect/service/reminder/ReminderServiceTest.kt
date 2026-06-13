package com.connect.service.reminder

import com.connect.service.reminder.dto.ReminderCreateRequest
import com.connect.service.reminder.entity.DateReminder
import com.connect.service.reminder.repository.ReminderRepository
import com.connect.service.reminder.service.ReminderService
import com.connect.service.user.domain.UserRole
import com.connect.service.user.domain.Users
import com.connect.service.user.repository.UserRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class ReminderServiceTest {

    @Mock
    private lateinit var reminderRepository: ReminderRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var javaMailSender: JavaMailSender

    @InjectMocks
    private lateinit var reminderService: ReminderService

    @Test
    @DisplayName("미인증 사용자도 알림을 등록할 수 있다 (이메일 미지정 시 계정 이메일)")
    fun `미인증도_등록_가능_계정이메일_사용`() {
        // Given: 인증(ROLE_VERIFIED) 없는 일반 사용자
        val user = Users(userId = "u1", email = "me@douzone.com", name = "홍길동", rawPassword = "x")
        whenever(userRepository.findByUserId("u1")).thenReturn(user)
        whenever(reminderRepository.save(any<DateReminder>())).thenAnswer { it.arguments[0] as DateReminder }

        val req = ReminderCreateRequest(LocalDate.of(2026, 9, 12), "오전 반차", null)
        val result = reminderService.create("u1", req)

        assertEquals("me@douzone.com", result.email)
        assertEquals("오전 반차", result.content)
        assertEquals(LocalDate.of(2026, 9, 12), result.reminderDate)
    }

    @Test
    @DisplayName("이메일을 지정하면 해당 이메일로 등록한다")
    fun `이메일_지정시_해당_이메일_사용`() {
        val user = Users(userId = "u1", email = "me@douzone.com", name = "홍길동", rawPassword = "x")
        whenever(userRepository.findByUserId("u1")).thenReturn(user)
        whenever(reminderRepository.save(any<DateReminder>())).thenAnswer { it.arguments[0] as DateReminder }

        val req = ReminderCreateRequest(LocalDate.of(2026, 9, 13), "오후 반차", "custom@douzone.com")
        val result = reminderService.create("u1", req)

        assertEquals("custom@douzone.com", result.email)
    }

    @Test
    @DisplayName("알림 내용이 비어 있으면 예외를 던진다")
    fun `빈_내용_예외`() {
        assertThrows<IllegalArgumentException> {
            reminderService.create("u1", ReminderCreateRequest(LocalDate.of(2026, 9, 12), "   ", null))
        }
        verify(reminderRepository, never()).save(any<DateReminder>())
    }

    @Test
    @DisplayName("인증(ROLE_VERIFIED) 사용자의 당일 알림은 이메일을 발송하고 발송 완료로 표시한다")
    fun `인증_사용자_알림_메일_발송`() {
        val today = LocalDate.of(2026, 9, 12)
        val reminder = DateReminder(
            id = 1L, userId = "u1", email = "a@douzone.com",
            reminderDate = today, content = "오전 반차", notified = false
        )
        whenever(reminderRepository.findByReminderDateAndNotifiedFalse(today)).thenReturn(listOf(reminder))
        val verifiedUser = Users(
            userId = "u1", email = "a@douzone.com", name = "홍길동", rawPassword = "x",
            roles = mutableSetOf(UserRole.ROLE_USER, UserRole.ROLE_VERIFIED)
        )
        whenever(userRepository.findByUserIdIn(any())).thenReturn(listOf(verifiedUser))

        val sent = reminderService.sendDueReminders(today)

        verify(javaMailSender).send(any<SimpleMailMessage>())
        assertTrue(reminder.notified)
        assertEquals(1, sent)
    }

    @Test
    @DisplayName("미인증 사용자의 알림은 이메일을 발송하지 않는다 (마이페이지 조회 전용)")
    fun `미인증_사용자_메일_미발송`() {
        val today = LocalDate.of(2026, 9, 12)
        val reminder = DateReminder(
            id = 2L, userId = "u2", email = "u2@kakao.local",
            reminderDate = today, content = "오전 반차", notified = false
        )
        whenever(reminderRepository.findByReminderDateAndNotifiedFalse(today)).thenReturn(listOf(reminder))
        // ROLE_VERIFIED 없는 일반 사용자
        val plainUser = Users(userId = "u2", email = "u2@kakao.local", name = "미인증", rawPassword = "x")
        whenever(userRepository.findByUserIdIn(any())).thenReturn(listOf(plainUser))

        val sent = reminderService.sendDueReminders(today)

        verify(javaMailSender, never()).send(any<SimpleMailMessage>())
        assertFalse(reminder.notified)
        assertEquals(0, sent)
    }
}
