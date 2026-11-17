package com.connect.service.user.service

import com.connect.service.user.domain.UserRole
import com.connect.service.user.domain.Users
import com.connect.service.user.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.util.UUID

/**
 * 로그인하지 않은 방문자용 게스트 계정.
 *
 * 게스트도 users 테이블에 한 줄로 만든다. 그래야 게시글/댓글/채팅이 회원과 똑같이 userId 로 동작한다.
 *  - userId : "guest_" + 랜덤 12자리 (회원가입에서는 이 접두사를 못 쓰게 막는다)
 *  - name   : "용감한 판다 42" 같은 랜덤 닉네임
 *  - 비밀번호: 아무도 모르는 랜덤 값을 암호화해 넣는다 → 아이디/비밀번호 로그인 불가
 * 본인 확인은 GuestCookieManager 가 발급한 서명된 쿠키로만 한다.
 */
@Service
class GuestService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    companion object {
        const val GUEST_ID_PREFIX = "guest_"

        private val ADJECTIVES = listOf(
            "용감한", "졸린", "배고픈", "신나는", "느긋한", "수줍은", "씩씩한", "엉뚱한",
            "다정한", "재빠른", "꼼꼼한", "명랑한", "조용한", "호기심많은", "든든한", "반짝이는",
        )
        private val ANIMALS = listOf(
            "판다", "수달", "고양이", "강아지", "펭귄", "다람쥐", "토끼", "부엉이",
            "고래", "여우", "햄스터", "코알라", "알파카", "거북이", "기린", "너구리",
        )
        private val random = SecureRandom()

        fun isGuestUserId(userId: String?): Boolean = userId?.startsWith(GUEST_ID_PREFIX) == true
    }

    /** 게스트 userId 로 게스트 계정을 찾는다. 게스트가 아니거나 정지된 계정이면 null. */
    @Transactional(readOnly = true)
    fun findActiveGuest(userId: String): Users? =
        userRepository.findByUserId(userId)
            ?.takeIf { it.roles.contains(UserRole.ROLE_GUEST) && it.enabled }

    @Transactional
    fun createGuest(): Users {
        // userId 는 UNIQUE 라 충돌하면 저장이 실패한다. 12자리 hex 라 사실상 겹치지 않지만 한 번 더 확인한다.
        var userId: String
        do {
            userId = GUEST_ID_PREFIX + UUID.randomUUID().toString().replace("-", "").take(12)
        } while (userRepository.findByUserId(userId) != null)

        return userRepository.save(
            Users(
                userId = userId,
                email = "",
                name = randomNickname(),
                rawPassword = passwordEncoder.encode(UUID.randomUUID().toString()),
                roles = mutableSetOf(UserRole.ROLE_GUEST),
            )
        )
    }

    private fun randomNickname(): String =
        "${ADJECTIVES[random.nextInt(ADJECTIVES.size)]} ${ANIMALS[random.nextInt(ANIMALS.size)]} ${random.nextInt(90) + 10}"
}
