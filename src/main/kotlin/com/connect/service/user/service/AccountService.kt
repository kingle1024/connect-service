package com.connect.service.user.service

import com.connect.service.board.repository.BoardRepository
import com.connect.service.user.domain.EmailVerification
import com.connect.service.user.domain.UserRole
import com.connect.service.user.domain.Users
import com.connect.service.user.dto.VerificationInfo
import com.connect.service.user.repository.EmailVerificationRepository
import com.connect.service.user.repository.UserRepository
import jakarta.transaction.Transactional
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.Timer
import kotlin.concurrent.schedule
import java.util.regex.Pattern

@Service
class AccountService(
    private val javaMailSender: JavaMailSender,
    private val emailVerificationRepository: EmailVerificationRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val boardRepository: BoardRepository
) {
    private val EMAIL_REGEX_PATTERN: Pattern = Pattern.compile(
            "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+\$"
        )
    private val VERIFICATION_CODE_EXPIRY_MINUTES: Long = 5 // 인증번호 유효 시간 5분
    private val DOUZONE_EMAIL_DOMAIN = "@douzone.com" // 인증 가능한 사내 이메일 도메인

    @Transactional
    fun sendVerificationCode(email: String): Boolean {
        // 1. 이메일 유효성 검증 (여기서는 기본적인 형식만, 더 복잡한 검증은 클라이언트와 서버 양쪽에서)
        if (!isValidEmail(email)) {
            throw IllegalArgumentException("유효하지 않은 이메일 주소입니다.")
        }

        // 2. 이메일이 등록된 사용자의 것인지 확인 (비밀번호 찾기 흐름에서는 필수)
        if (!isRegisteredEmail(email)) {
            throw IllegalArgumentException("등록되지 않은 이메일 주소입니다.")
        }

        val existingVerifications = emailVerificationRepository
            .findAllByEmailAndExpiresAtAfterAndIsUsedFalse(email, LocalDateTime.now())
        existingVerifications.forEach { it.isUsed = true } // 사용됨 상태로 변경
        emailVerificationRepository.saveAll(existingVerifications)


        // 3. 6자리 랜덤 인증번호 생성
        val verificationCode = generateRandomNumber(6)
        val expiryTime = LocalDateTime.now().plusMinutes(VERIFICATION_CODE_EXPIRY_MINUTES)

        // 4. 인증번호와 만료 시간을 캐시에 저장
        val newVerification = EmailVerification(
            email = email,
            verificationCode = verificationCode,
            expiresAt = expiryTime
        )
        emailVerificationRepository.save(newVerification)

        // 5. 이메일 발송
        val message = SimpleMailMessage()
        message.setTo(email)
        message.setSubject("[같이타] 비밀번호 찾기 인증번호")
        message.setText("안녕하세요. \n\n[같이타] 비밀번호 찾기 인증번호는 [ $verificationCode ] 입니다. \n\n이 인증번호는 $VERIFICATION_CODE_EXPIRY_MINUTES 분 후 만료됩니다.")
        javaMailSender.send(message)

        println("인증번호 '$verificationCode'가 '$email'로 발송되었습니다. (유효시간: $VERIFICATION_CODE_EXPIRY_MINUTES 분)")

        return true
    }

    /**
     * 이메일과 인증번호가 유효한지 확인합니다.
     * @param email 사용자 이메일
     * @param code 사용자가 입력한 인증번호
     * @return 인증 성공 여부
     */
    @Transactional
    fun verifyCode(email: String, code: String): Boolean {
        val verification = emailVerificationRepository
            .findByEmailAndVerificationCodeAndExpiresAtAfterAndIsUsedFalse(email, code, LocalDateTime.now())
            .orElse(null) ?: return false

        // 인증번호 일치 및 만료 시간 확인
        verification.isUsed = true
        emailVerificationRepository.save(verification)
        return true
    }

    /**
     * 비밀번호를 재설정합니다. (실제 구현에서는 DB 업데이트 로직 필요)
     * @param email 비밀번호를 재설정할 이메일
     * @param newPassword 새 비밀번호
     * @return 비밀번호 재설정 성공 여부
     */
    @Transactional
    fun resetPassword(email: String, newPassword: String): Boolean {
        val user = userRepository.findByEmail(email).orElse(null)
        if (user == null) {
            // 이메일에 해당하는 사용자가 없으므로, 재설정할 수 없습니다.
            throw IllegalArgumentException("해당 이메일로 등록된 사용자를 찾을 수 없습니다.")
        }

        // 2. 새 비밀번호를 해싱하여 저장
        val encodedPassword = passwordEncoder.encode(newPassword)
        user.rawPassword = encodedPassword // Users 엔티티의 rawPassword 필드를 업데이트
        userRepository.save(user) // 변경된 사용자 정보 저장

        println("'$email' 사용자의 비밀번호가 성공적으로 재설정되었습니다.")
        return true

    }

    /**
     * 더존 이메일(@douzone.com) 인증번호 발송.
     * 비밀번호 찾기와 달리 가입된 이메일일 필요는 없지만, 반드시 @douzone.com 도메인이어야 합니다.
     */
    @Transactional
    fun sendDouzoneVerificationCode(email: String): Boolean {
        if (!isValidEmail(email)) {
            throw IllegalArgumentException("유효하지 않은 이메일 주소입니다.")
        }
        if (!email.lowercase().endsWith(DOUZONE_EMAIL_DOMAIN)) {
            throw IllegalArgumentException("@douzone.com 이메일만 인증할 수 있습니다.")
        }

        // 기존 미사용 인증코드 무효화
        val existingVerifications = emailVerificationRepository
            .findAllByEmailAndExpiresAtAfterAndIsUsedFalse(email, LocalDateTime.now())
        existingVerifications.forEach { it.isUsed = true }
        emailVerificationRepository.saveAll(existingVerifications)

        val verificationCode = generateRandomNumber(6)
        val expiryTime = LocalDateTime.now().plusMinutes(VERIFICATION_CODE_EXPIRY_MINUTES)
        emailVerificationRepository.save(
            EmailVerification(email = email, verificationCode = verificationCode, expiresAt = expiryTime)
        )

        val message = SimpleMailMessage()
        message.setTo(email)
        message.setSubject("[같이타] 더존 이메일 인증번호")
        message.setText("안녕하세요.\n\n[같이타] 더존 이메일 인증번호는 [ $verificationCode ] 입니다.\n\n이 인증번호는 $VERIFICATION_CODE_EXPIRY_MINUTES 분 후 만료됩니다.")
        javaMailSender.send(message)

        return true
    }

    /**
     * 더존 이메일 인증 확정. 인증번호가 유효하면 해당 사용자에게 ROLE_VERIFIED 를 부여합니다.
     * @return 인증 성공 시 갱신된 사용자, 인증번호 불일치/만료 시 null
     */
    @Transactional
    fun verifyDouzoneEmail(userId: String, email: String, code: String): Users? {
        if (!email.lowercase().endsWith(DOUZONE_EMAIL_DOMAIN)) {
            throw IllegalArgumentException("@douzone.com 이메일만 인증할 수 있습니다.")
        }
        if (!verifyCode(email, code)) {
            return null
        }
        val user = userRepository.findByUserId(userId)
            ?: throw IllegalArgumentException("사용자를 찾을 수 없습니다.")
        user.roles.add(UserRole.ROLE_VERIFIED)
        return userRepository.save(user)
    }

    /**
     * 본인 이름(별칭)을 변경합니다. 새 글 작성 시 이 이름이 사용됩니다.
     * (기존 게시글의 작성자명은 작성 당시 스냅샷이라 변경되지 않습니다)
     * @param userId 변경 대상 사용자 ID (JWT에서 추출)
     * @param newName 새 이름
     * @return 변경된 사용자
     */
    @Transactional
    fun updateUserName(userId: String, newName: String): Users {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("이름은 비어 있을 수 없습니다.")
        }
        val user = userRepository.findByUserId(userId)
            ?: throw IllegalArgumentException("사용자를 찾을 수 없습니다.")
        user.name = trimmed
        val saved = userRepository.save(user)
        // 이미 작성한 게시글의 작성자명도 함께 변경 (모집 탭에 표시되는 이름 일괄 반영)
        boardRepository.updateUserNameByUserId(userId, trimmed)
        return saved
    }

    // 6자리 랜덤 숫자 문자열 생성
    private fun generateRandomNumber(length: Int): String {
        val random = SecureRandom()
        val builder = StringBuilder(length)
        for (i in 0 until length) {
            builder.append(random.nextInt(10)) // 0-9 사이 숫자
        }
        return builder.toString()
    }


    // 간단한 이메일 형식 유효성 검증
    private fun isValidEmail(email: String): Boolean {
        return EMAIL_REGEX_PATTERN.matcher(email).matches()
    }

    // 가상으로 이메일 등록 여부 확인 (실제로는 DB 조회)
    private fun isRegisteredEmail(email: String): Boolean {
        return userRepository.findByEmail(email).isPresent
    }
}
