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
import java.time.LocalDate
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class ReminderServiceTest {

    @Mock
    private lateinit var reminderRepository: ReminderRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @InjectMocks
    private lateinit var reminderService: ReminderService

    @Test
    @DisplayName("이메일 미지정 시 계정 이메일로 알림을 등록한다 (등록은 누구나 가능)")
    fun `이메일_미지정시_계정이메일_사용`() {
        val user = Users(
            userId = "u1", email = "me@douzone.com", name = "홍길동", rawPassword = "x",
            roles = mutableSetOf(UserRole.ROLE_USER, UserRole.ROLE_VERIFIED)
        )
        whenever(userRepository.findByUserId("u1")).thenReturn(user)
        whenever(reminderRepository.save(any<DateReminder>())).thenAnswer { it.arguments[0] as DateReminder }

        val req = ReminderCreateRequest(LocalDate.of(2026, 9, 12), "오전 반차", null)
        val result = reminderService.create("u1", req)

        assertEquals("me@douzone.com", result.email)
        assertEquals("오전 반차", result.content)
        assertEquals(LocalDate.of(2026, 9, 12), result.reminderDate)
    }

    @Test
    @DisplayName("요청에 이메일을 넣어도 무시하고 항상 계정(인증) 이메일을 사용한다")
    fun `요청_이메일_무시하고_계정이메일_사용`() {
        val user = Users(
            userId = "u1", email = "me@douzone.com", name = "홍길동", rawPassword = "x",
            roles = mutableSetOf(UserRole.ROLE_USER, UserRole.ROLE_VERIFIED)
        )
        whenever(userRepository.findByUserId("u1")).thenReturn(user)
        whenever(reminderRepository.save(any<DateReminder>())).thenAnswer { it.arguments[0] as DateReminder }

        // 요청에 다른 이메일을 넣어도 계정 이메일로 등록되어야 함
        val req = ReminderCreateRequest(LocalDate.of(2026, 9, 13), "오후 반차", "custom@douzone.com")
        val result = reminderService.create("u1", req)

        assertEquals("me@douzone.com", result.email)
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
    @DisplayName("더존 이메일 인증(ROLE_VERIFIED)이 없으면 알림 등록을 거부한다")
    fun `미인증_사용자_거부`() {
        // Given: ROLE_VERIFIED 없는 일반 사용자
        val user = Users(userId = "u1", email = "u1@kakao.local", name = "미인증", rawPassword = "x")
        whenever(userRepository.findByUserId("u1")).thenReturn(user)

        // When & Then
        assertThrows<IllegalArgumentException> {
            reminderService.create("u1", ReminderCreateRequest(LocalDate.of(2026, 9, 12), "오전 반차", null))
        }
        verify(reminderRepository, never()).save(any<DateReminder>())
    }
}
