package com.connect.service.user

import com.connect.service.board.repository.BoardRepository
import com.connect.service.user.domain.EmailVerification
import com.connect.service.user.domain.UserRole
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
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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

    @Mock
    private lateinit var boardRepository: BoardRepository

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
        // 기존 게시글 작성자명도 동기화 호출
        verify(boardRepository).updateUserNameByUserId("kakao_1", "새이름")
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

    @Test
    @DisplayName("@douzone.com 이 아닌 이메일은 인증번호 발송이 거부된다")
    fun `더존_도메인_아니면_발송_예외`() {
        assertThrows<IllegalArgumentException> {
            accountService.sendDouzoneVerificationCode("user@gmail.com")
        }
        // 도메인 검증에서 막히므로 메일 발송/저장은 호출되지 않음
        verify(emailVerificationRepository, never()).save(any<EmailVerification>())
    }

    @Test
    @DisplayName("더존 이메일 인증 성공 시 ROLE_VERIFIED 가 부여된다")
    fun `더존_인증_성공_ROLE_VERIFIED_부여`() {
        // Given: 유효한 인증번호가 존재
        val email = "hong@douzone.com"
        val code = "123456"
        val verification = EmailVerification(
            email = email,
            verificationCode = code,
            expiresAt = LocalDateTime.now().plusMinutes(5)
        )
        whenever(
            emailVerificationRepository.findByEmailAndVerificationCodeAndExpiresAtAfterAndIsUsedFalse(
                eq(email), eq(code), any()
            )
        ).thenReturn(Optional.of(verification))
        whenever(emailVerificationRepository.save(any<EmailVerification>())).thenAnswer { it.arguments[0] }

        val user = Users(userId = "kakao_1", email = "kakao_1@kakao.local", name = "홍길동", rawPassword = "x")
        whenever(userRepository.findByUserId("kakao_1")).thenReturn(user)
        whenever(userRepository.save(any<Users>())).thenAnswer { it.arguments[0] as Users }

        // When
        val result = accountService.verifyDouzoneEmail("kakao_1", email, code)

        // Then: ROLE_VERIFIED 부여
        assertNotNull(result)
        assertTrue(result.roles.contains(UserRole.ROLE_VERIFIED))
    }

    @Test
    @DisplayName("인증번호가 틀리면 null 을 반환하고 ROLE_VERIFIED 를 부여하지 않는다")
    fun `더존_인증_실패_null`() {
        // Given: 일치하는 인증코드 없음
        val email = "hong@douzone.com"
        whenever(
            emailVerificationRepository.findByEmailAndVerificationCodeAndExpiresAtAfterAndIsUsedFalse(
                eq(email), eq("000000"), any()
            )
        ).thenReturn(Optional.empty())

        // When
        val result = accountService.verifyDouzoneEmail("kakao_1", email, "000000")

        // Then
        assertEquals(null, result)
        verify(userRepository, never()).save(any<Users>())
    }
}
