package com.connect.service.user

import com.connect.service.user.domain.Users
import com.connect.service.user.dto.KakaoUserInfo
import com.connect.service.user.repository.UserRepository
import com.connect.service.user.service.KakaoOAuthService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class KakaoOAuthServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var passwordEncoder: org.springframework.security.crypto.password.PasswordEncoder

    @InjectMocks
    private lateinit var kakaoOAuthService: KakaoOAuthService

    @Test
    @DisplayName("신규 카카오 사용자는 kakao_ 접두사 userId로 새로 생성된다")
    fun `신규_카카오_사용자_생성`() {
        // Given: 해당 카카오 사용자가 아직 없음
        val info = KakaoUserInfo(id = "123456", email = "user@kakao.com", nickname = "홍길동", profileImage = "http://img/p.jpg")
        whenever(userRepository.findByUserId("kakao_123456")).thenReturn(null)
        whenever(passwordEncoder.encode(any())).thenReturn("encoded-random")
        // save는 전달받은 엔티티를 그대로 반환하도록 설정
        whenever(userRepository.save(any<Users>())).thenAnswer { it.arguments[0] as Users }

        // When
        val result = kakaoOAuthService.loginOrRegister(info)

        // Then
        assertEquals("kakao_123456", result.userId)
        assertEquals("user@kakao.com", result.email)
        assertEquals("홍길동", result.name)
        assertEquals("http://img/p.jpg", result.profileUrl)
        verify(userRepository).save(any<Users>())
    }

    @Test
    @DisplayName("이메일/닉네임 미동의 시 대체 값으로 사용자를 생성한다")
    fun `이메일_닉네임_없을때_대체값_생성`() {
        // Given: 이메일과 닉네임이 모두 null
        val info = KakaoUserInfo(id = "999", email = null, nickname = null, profileImage = null)
        whenever(userRepository.findByUserId("kakao_999")).thenReturn(null)
        whenever(passwordEncoder.encode(any())).thenReturn("encoded-random")
        whenever(userRepository.save(any<Users>())).thenAnswer { it.arguments[0] as Users }

        // When
        val result = kakaoOAuthService.loginOrRegister(info)

        // Then: 대체 이메일/닉네임이 채워진다
        assertEquals("kakao_999", result.userId)
        assertTrue(result.email.endsWith("@kakao.local"))
        assertEquals("카카오사용자", result.name)
    }

    @Test
    @DisplayName("동시 요청으로 INSERT 가 중복되면 기존 사용자를 재조회해 반환한다")
    fun `동시_생성_중복시_재조회_반환`() {
        // Given: 최초 조회 시엔 없음 → save 가 중복(DataIntegrityViolation) → 재조회 시엔 존재
        val info = KakaoUserInfo(id = "777", email = null, nickname = "둘리", profileImage = null)
        val concurrentlyCreated = Users(userId = "kakao_777", email = "x@kakao.local", name = "둘리", rawPassword = "x")
        whenever(userRepository.findByUserId("kakao_777"))
            .thenReturn(null) // 1차 조회: 없음
            .thenReturn(concurrentlyCreated) // catch 내 재조회: 존재
        whenever(passwordEncoder.encode(any())).thenReturn("encoded-random")
        whenever(userRepository.save(any<Users>()))
            .thenThrow(org.springframework.dao.DataIntegrityViolationException("duplicate"))

        // When
        val result = kakaoOAuthService.loginOrRegister(info)

        // Then: 예외를 삼키고 기존 사용자를 반환
        assertEquals(concurrentlyCreated, result)
    }

    @Test
    @DisplayName("기존 카카오 사용자는 새로 생성하지 않고 재사용한다")
    fun `기존_카카오_사용자_재사용`() {
        // Given: 동일한 카카오 사용자가 이미 존재
        val existing = Users(
            userId = "kakao_123456",
            email = "user@kakao.com",
            name = "홍길동",
            rawPassword = "stored"
        )
        whenever(userRepository.findByUserId("kakao_123456")).thenReturn(existing)
        val info = KakaoUserInfo(id = "123456", email = "user@kakao.com", nickname = "홍길동", profileImage = null)

        // When
        val result = kakaoOAuthService.loginOrRegister(info)

        // Then: 기존 사용자를 그대로 반환하고 저장은 호출하지 않는다
        assertEquals(existing, result)
        verify(userRepository, never()).save(any<Users>())
    }
}
