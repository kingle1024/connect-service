package com.connect.service.user.service

import com.connect.service.user.domain.Users
import com.connect.service.user.dto.KakaoUserInfo
import com.connect.service.user.repository.UserRepository
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate
import java.util.UUID

@Service
class KakaoOAuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    companion object {
        private val log = KotlinLogging.logger {}
    }

    @Value("\${spring.security.oauth2.client.registration.kakao.client-id}")
    private lateinit var clientId: String

    @Value("\${spring.security.oauth2.client.registration.kakao.client-secret}")
    private lateinit var clientSecret: String

    @Value("\${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private lateinit var redirectUri: String

    @Value("\${spring.security.oauth2.client.provider.kakao.token-uri}")
    private lateinit var tokenUri: String

    @Value("\${spring.security.oauth2.client.provider.kakao.user-info-uri}")
    private lateinit var userInfoUri: String

    private val restTemplate = RestTemplate()

    // 인가 코드(code)를 카카오 액세스 토큰으로 교환
    fun exchangeCodeForToken(code: String): String {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
        }
        val body = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "authorization_code")
            add("client_id", clientId)
            add("client_secret", clientSecret)
            add("redirect_uri", redirectUri)
            add("code", code)
        }

        @Suppress("UNCHECKED_CAST")
        val response = restTemplate.postForObject(
            tokenUri,
            HttpEntity(body, headers),
            Map::class.java
        ) as? Map<String, Any> ?: throw IllegalStateException("카카오 토큰 응답이 비어 있습니다.")

        return response["access_token"] as? String
            ?: throw IllegalStateException("카카오 액세스 토큰을 찾을 수 없습니다.")
    }

    // 카카오 액세스 토큰으로 사용자 정보 조회
    fun fetchKakaoUser(kakaoAccessToken: String): KakaoUserInfo {
        val headers = HttpHeaders().apply {
            setBearerAuth(kakaoAccessToken)
        }

        @Suppress("UNCHECKED_CAST")
        val response = restTemplate.exchange(
            userInfoUri,
            HttpMethod.GET,
            HttpEntity<Void>(headers),
            Map::class.java
        ).body as? Map<String, Any> ?: throw IllegalStateException("카카오 사용자 정보 응답이 비어 있습니다.")

        val id = response["id"]?.toString()
            ?: throw IllegalStateException("카카오 회원번호를 찾을 수 없습니다.")

        val account = response["kakao_account"] as? Map<*, *>
        val profile = account?.get("profile") as? Map<*, *>

        return KakaoUserInfo(
            id = id,
            email = account?.get("email") as? String,
            nickname = profile?.get("nickname") as? String,
            profileImage = profile?.get("profile_image_url") as? String
        )
    }

    // 카카오 사용자를 우리 서비스 사용자(Users)로 연결. 없으면 신규 생성
    // 메서드 단위 @Transactional 을 두지 않음: save()가 자체 트랜잭션으로 실행돼야
    // 중복 INSERT 실패 시 해당 트랜잭션만 롤백되고, catch 의 재조회가 새 트랜잭션에서 성공한다.
    fun loginOrRegister(info: KakaoUserInfo): Users {
        // 소셜 사용자는 별도 스키마 변경 없이 userId 접두사("kakao_")로 식별
        val userId = "kakao_${info.id}"

        userRepository.findByUserId(userId)?.let {
            log.info("기존 카카오 사용자 로그인: userId = {}", userId)
            return it
        }

        val newUser = Users(
            userId = userId,
            email = info.email ?: "$userId@kakao.local", // 이메일 미동의 시 대체 이메일 사용
            name = info.nickname ?: "카카오사용자",
            // 소셜 로그인은 비밀번호가 없으므로 임의의 난수를 암호화하여 저장(비밀번호 로그인 차단 효과)
            rawPassword = passwordEncoder.encode(UUID.randomUUID().toString()),
            profileUrl = info.profileImage
        )

        return try {
            log.info("신규 카카오 사용자 생성: userId = {}", userId)
            userRepository.save(newUser)
        } catch (e: DataIntegrityViolationException) {
            // 동시 요청으로 다른 트랜잭션이 먼저 같은 사용자를 생성한 경우(중복 INSERT) 기존 사용자를 반환
            log.warn("카카오 사용자 동시 생성 감지, 기존 사용자 재조회: userId = {}", userId)
            userRepository.findByUserId(userId) ?: throw e
        }
    }
}
