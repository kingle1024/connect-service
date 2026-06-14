package com.connect.service.user

import com.connect.service.user.repository.UserRepository
import com.connect.service.user.service.ProfileImageService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import kotlin.test.assertEquals

class ProfileImageServiceTest {

    // 설정값은 비워도 extensionOf 는 순수 함수라 동작 (s3 클라이언트는 lazy 라 생성 안 됨)
    private val service = ProfileImageService(
        userRepository = mock<UserRepository>(),
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
}
