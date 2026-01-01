package com.connect.service.user.repository

import com.connect.service.user.domain.EmailVerification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

@Repository
interface EmailVerificationRepository : JpaRepository<EmailVerification, Long> {

    // 특정 이메일로 발급된, 만료되지 않고 사용되지 않은 최신 인증 코드를 찾습니다.
    fun findTopByEmailAndExpiresAtAfterAndIsUsedFalseOrderByCreatedAtDesc(
        email: String,
        currentTime: LocalDateTime
    ): Optional<EmailVerification>

    // 특정 이메일로 발급된 모든, 만료되지 않고 사용되지 않은 코드를 찾아서 일괄 처리할 수도 있습니다.
    fun findAllByEmailAndExpiresAtAfterAndIsUsedFalse(email: String, currentTime: LocalDateTime): List<EmailVerification>

    // 이메일과 코드가 일치하고, 만료되지 않았으며, 사용되지 않은 코드를 찾습니다.
    fun findByEmailAndVerificationCodeAndExpiresAtAfterAndIsUsedFalse(
        email: String,
        verificationCode: String,
        currentTime: LocalDateTime
    ): Optional<EmailVerification>
}
