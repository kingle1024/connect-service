package com.connect.service.inquiry.service

import com.connect.service.inquiry.dto.InquiryCreateRequest
import com.connect.service.inquiry.dto.InquiryDto
import com.connect.service.inquiry.entity.InquiryMst
import com.connect.service.inquiry.repository.InquiryRepository
import com.connect.service.user.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

/**
 * 앱에서 접수하는 개선/버그 요청.
 *
 * 로그인 없이도 접수할 수 있고, 작성자는 둘 중 하나로 구분한다.
 *  - 회원   : 로그인 아이디(userId)
 *  - 비회원 : 단말이 만들어 캐시에 보관하는 guestKey
 * 어느 쪽이든 조회할 때는 "본인 것"만 돌려준다.
 *
 * 관리자용 기능은 AdminInquiryService 가 담당한다.
 */
@Service
class InquiryService(
    private val inquiryRepository: InquiryRepository,
    private val userRepository: UserRepository
) {

    companion object {
        const val MAX_TITLE_LENGTH = 200
        const val MAX_CONTENT_LENGTH = 5000
        const val MAX_EMAIL_LENGTH = 255

        /** 비회원 이름 자리에 넣는 표기. 백오피스 목록에서 회원과 구분된다. */
        private const val GUEST_NAME = "비회원"

        // 단말이 만든 키라 형식만 확인한다. (UUID / 랜덤 hex 를 모두 받아들이는 범위)
        private val GUEST_KEY_PATTERN = Regex("^[A-Za-z0-9_-]{8,64}$")
        private val EMAIL_PATTERN = Regex("^[A-Za-z0-9_!#$%&'*+/=?`{|}~^.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }

    /**
     * 문의 접수.
     *
     * @param userId 로그인한 사용자의 아이디. 비로그인 접수면 null 이고 이때는 request.guestKey 가 필요하다.
     */
    @Transactional
    fun create(userId: String?, request: InquiryCreateRequest): InquiryDto {
        val title = request.title.trim()
        val content = request.content.trim()

        if (title.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "제목을 입력해주세요.")
        }
        if (title.length > MAX_TITLE_LENGTH) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "제목은 ${MAX_TITLE_LENGTH}자 이하여야 합니다.")
        }
        if (content.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "내용을 입력해주세요.")
        }
        if (content.length > MAX_CONTENT_LENGTH) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "내용은 ${MAX_CONTENT_LENGTH}자 이하여야 합니다.")
        }

        val inquiry = if (userId != null) {
            val user = userRepository.findByUserId(userId)
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "계정을 찾을 수 없습니다.")

            InquiryMst(
                userId = user.userId,
                // 이름이 비어 있는 계정도 있어서 아이디로 대체한다.
                userName = user.name.takeIf { it.isNotBlank() } ?: user.userId,
                type = request.type,
                title = title,
                content = content
            )
        } else {
            InquiryMst(
                guestKey = normalizedGuestKey(request.guestKey),
                guestEmail = normalizedGuestEmail(request.guestEmail),
                userName = GUEST_NAME,
                type = request.type,
                title = title,
                content = content
            )
        }

        return InquiryDto.from(inquiryRepository.save(inquiry))
    }

    /**
     * 내 문의 내역 (최신순).
     *
     * 회원이면 로그인 아이디로, 비회원이면 단말의 guestKey 로 찾는다.
     * 비회원인데 아직 키가 없으면(=이 단말에서 접수한 적이 없으면) 빈 목록이다.
     */
    @Transactional(readOnly = true)
    fun getMyInquiries(userId: String?, guestKey: String?): List<InquiryDto> {
        if (userId != null) {
            return inquiryRepository.findAllByUserIdAndIsDeletedFalseOrderByIdDesc(userId).map { InquiryDto.from(it) }
        }
        val key = guestKey?.trim()?.takeIf { it.isNotEmpty() } ?: return emptyList()
        return inquiryRepository.findAllByGuestKeyAndIsDeletedFalseOrderByIdDesc(key).map { InquiryDto.from(it) }
    }

    @Transactional(readOnly = true)
    fun getMyInquiry(userId: String?, guestKey: String?, id: Long): InquiryDto {
        val inquiry = inquiryRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "문의를 찾을 수 없습니다: $id") }

        val mine = if (userId != null) {
            inquiry.userId == userId
        } else {
            // guestKey 가 비어 있으면 어떤 문의와도 매칭되지 않게 막는다.
            val key = guestKey?.trim()?.takeIf { it.isNotEmpty() }
            key != null && inquiry.guestKey == key
        }

        // 남의 문의는 존재 여부도 알려주지 않는다.
        if (!mine || inquiry.isDeleted) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "문의를 찾을 수 없습니다: $id")
        }
        return InquiryDto.from(inquiry)
    }

    private fun normalizedGuestKey(rawGuestKey: String?): String {
        val key = rawGuestKey?.trim().orEmpty()
        if (!GUEST_KEY_PATTERN.matches(key)) {
            // 앱이 항상 만들어 보내는 값이라 사용자가 고칠 수 있는 문제는 아니다.
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "요청을 접수할 수 없습니다. 앱을 새로고침한 뒤 다시 시도해주세요.")
        }
        return key
    }

    /** 선택 입력값이라 비어 있으면 null, 넣었다면 형식은 확인한다. */
    private fun normalizedGuestEmail(rawEmail: String?): String? {
        val email = rawEmail?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (email.length > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matches(email)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "이메일 주소 형식을 확인해주세요.")
        }
        return email
    }
}
