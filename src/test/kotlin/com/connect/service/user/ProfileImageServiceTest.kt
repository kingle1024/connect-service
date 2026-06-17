package com.connect.service.user

import com.connect.service.user.domain.Users
import com.connect.service.user.repository.UserRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import com.connect.service.user.service.ProfileImageService
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProfileImageServiceTest {

    private val userRepository = mock<UserRepository>()

    // 설정값은 비워도 extensionOf 는 순수 함수라 동작 (s3 클라이언트는 lazy 라 생성 안 됨)
    private val service = ProfileImageService(
        userRepository = userRepository,
        endpoint = "",
        accessKey = "",
        secretKey = "",
        bucket = "",
        publicBaseUrl = ""
    )

    @Test
    @DisplayName("contentType 에 따라 파일 확장자를 매핑한다")
    fun `확장자_매핑`() {
        assertEquals("png", service.extensionOf("image/png"))
        assertEquals("gif", service.extensionOf("image/gif"))
        assertEquals("webp", service.extensionOf("image/webp"))
        assertEquals("jpg", service.extensionOf("image/jpeg"))
        assertEquals("jpg", service.extensionOf(null))
        assertEquals("jpg", service.extensionOf("application/octet-stream"))
    }

    @Test
    @DisplayName("프로필 사진 삭제 시 profileUrl 을 null 로 비운다 (R2 미설정이어도 참조 해제는 성공)")
    fun `삭제시_profileUrl_을_null_로_비운다`() {
        val user = Users(
            userId = "u1",
            email = "u1@douzone.com",
            name = "테스터",
            rawPassword = "encoded",
            profileUrl = "https://pub-xxxx.r2.dev/profile/u1_123.jpg"
        )
        whenever(userRepository.findByUserId("u1")).thenReturn(user)
        whenever(userRepository.save(any<Users>())).thenAnswer { it.arguments[0] as Users }

        val result = service.clearProfileImage("u1")

        // publicBaseUrl 이 비어 있어 R2 삭제는 건너뛰고, 참조만 해제된다
        assertNull(result.profileUrl)
    }
}
