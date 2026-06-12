package com.connect.service.user

import com.connect.service.user.domain.Users
import com.connect.service.user.repository.EmailVerificationRepository
import com.connect.service.user.repository.UserRepository
import com.connect.service.user.service.AccountService
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
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.security.crypto.password.PasswordEncoder
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class AccountServiceTest {

    @Mock
    private lateinit var javaMailSender: JavaMailSender

    @Mock
    private lateinit var emailVerificationRepository: EmailVerificationRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @InjectMocks
    private lateinit var accountService: AccountService

    @Test
    @DisplayName("이름 변경 시 앞뒤 공백을 제거하고 저장한다")
    fun `이름_변경_성공`() {
        // Given
        val user = Users(userId = "kakao_1", email = "a@b.com", name = "이전이름", rawPassword = "x")
        whenever(userRepository.findByUserId("kakao_1")).thenReturn(user)
        whenever(userRepository.save(any<Users>())).thenAnswer { it.arguments[0] as Users }

        // When
        val result = accountService.updateUserName("kakao_1", "  새이름  ")

        // Then: 공백 제거된 이름으로 변경
        assertEquals("새이름", result.name)
        verify(userRepository).save(any<Users>())
    }

    @Test
    @DisplayName("빈 이름이면 예외를 던지고 저장하지 않는다")
    fun `빈_이름_예외`() {
        // When & Then
        assertThrows<IllegalArgumentException> {
            accountService.updateUserName("kakao_1", "   ")
        }
        verify(userRepository, never()).save(any<Users>())
    }

    @Test
    @DisplayName("존재하지 않는 사용자면 예외를 던진다")
    fun `사용자_없음_예외`() {
        // Given
        whenever(userRepository.findByUserId("nope")).thenReturn(null)

        // When & Then
        assertThrows<IllegalArgumentException> {
            accountService.updateUserName("nope", "이름")
        }
    }
}
