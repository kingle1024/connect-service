package com.connect.service.user.service

import com.connect.service.user.domain.Users
import com.connect.service.user.repository.UserRepository
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.net.URI
import java.util.Base64

// 프로필 이미지를 Cloudflare R2(S3 호환)에 업로드하고 공개 URL을 Users.profileUrl 에 저장한다.
@Service
class ProfileImageService(
    private val userRepository: UserRepository,
    @Value("\${r2.endpoint:}") private val endpoint: String,
    @Value("\${r2.access-key:}") private val accessKey: String,
    @Value("\${r2.secret-key:}") private val secretKey: String,
    @Value("\${r2.bucket:}") private val bucket: String,
    @Value("\${r2.public-base-url:}") private val publicBaseUrl: String
) {
    private val log = KotlinLogging.logger {}

    // 설정이 채워졌을 때만 클라이언트 생성 (미설정 시 앱 기동에는 영향 없이, 업로드 시점에만 에러)
    private val s3Client: S3Client by lazy {
        check(endpoint.isNotBlank() && accessKey.isNotBlank() && secretKey.isNotBlank() && bucket.isNotBlank()) {
            "이미지 저장소(R2) 설정이 필요합니다."
        }
        S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .region(Region.of("auto")) // R2 는 region "auto"
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
            // R2 호환: path-style 접근(버킷을 경로로) 사용
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .build()
    }

    @Transactional
    fun updateProfileImage(userId: String, imageBase64: String, contentType: String?): Users {
        val user = userRepository.findByUserId(userId)
            ?: throw IllegalArgumentException("사용자를 찾을 수 없습니다.")

        val bytes = try {
            Base64.getDecoder().decode(stripDataUri(imageBase64).trim())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("이미지 데이터가 올바르지 않습니다.")
        }
        if (bytes.isEmpty()) {
            throw IllegalArgumentException("이미지 데이터가 비어 있습니다.")
        }

        val ext = extensionOf(contentType)
        val key = "profile/${userId}_${System.currentTimeMillis()}.$ext"

        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType ?: "image/jpeg")
                .build(),
            RequestBody.fromBytes(bytes)
        )

        user.profileUrl = "${publicBaseUrl.trimEnd('/')}/$key"
        return userRepository.save(user)
    }

    // 프로필 사진 삭제 (기본 이미지로 되돌리기). R2 객체를 삭제하고 profileUrl 을 비워 기본 아이콘이 보이게 한다.
    @Transactional
    fun clearProfileImage(userId: String): Users {
        val user = userRepository.findByUserId(userId)
            ?: throw IllegalArgumentException("사용자를 찾을 수 없습니다.")

        // R2 실제 객체도 삭제(best-effort). 실패해도 참조 해제는 진행해 기본 이미지로 되돌린다.
        val currentUrl = user.profileUrl
        if (!currentUrl.isNullOrBlank() && publicBaseUrl.isNotBlank()) {
            val prefix = "${publicBaseUrl.trimEnd('/')}/"
            if (currentUrl.startsWith(prefix)) {
                val key = currentUrl.removePrefix(prefix)
                try {
                    s3Client.deleteObject(
                        DeleteObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build()
                    )
                } catch (e: Exception) {
                    // R2 미설정/네트워크 오류 등은 무시하고 참조 해제만 진행
                    log.warn("R2 프로필 객체 삭제 실패(무시): key=$key, ${e.message}")
                }
            }
        }

        user.profileUrl = null
        return userRepository.save(user)
    }

    // data URI(data:image/png;base64,xxxx) 형태면 콤마 뒤 실제 base64만 추출
    private fun stripDataUri(s: String): String = if (s.contains(",")) s.substringAfter(",") else s

    fun extensionOf(contentType: String?): String = when (contentType?.lowercase()) {
        "image/png" -> "png"
        "image/gif" -> "gif"
        "image/webp" -> "webp"
        else -> "jpg"
    }
}
