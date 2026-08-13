package com.connect.service.admin.service

import com.connect.service.admin.dto.PageResponse
import com.connect.service.inquiry.dto.InquiryAnswerRequest
import com.connect.service.inquiry.dto.InquiryDto
import com.connect.service.inquiry.entity.InquiryMst
import com.connect.service.inquiry.entity.InquiryStatus
import com.connect.service.inquiry.entity.InquiryType
import com.connect.service.inquiry.repository.InquiryRepository
import mu.KotlinLogging
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime

/**
 * 백오피스 문의(개선/버그 요청) 관리.
 *
 * 접근 통제는 SecurityConfig 의 관리자 경로 규칙(hasRole("ADMIN"))이 담당한다.
 */
@Service
class AdminInquiryService(
    private val inquiryRepository: InquiryRepository,
    private val javaMailSender: JavaMailSender
) {

    companion object {
        const val MAX_ANSWER_LENGTH = 5000

        /** 사이드바 배지에 세는 "아직 손대지 않은" 상태들 */
        private val PENDING_STATUSES = listOf(InquiryStatus.RECEIVED, InquiryStatus.IN_PROGRESS)

        private val log = KotlinLogging.logger {}
    }

    @Transactional(readOnly = true)
    fun search(
        keyword: String?,
        type: InquiryType?,
        status: InquiryStatus?,
        isDeleted: Boolean?,
        pageable: Pageable
    ): PageResponse<InquiryDto> {
        val page = inquiryRepository.searchForAdmin(keyword, type, status, isDeleted, pageable)
        return PageResponse.from(page) { InquiryDto.from(it) }
    }

    @Transactional(readOnly = true)
    fun get(id: Long): InquiryDto = InquiryDto.from(findInquiry(id))

    /** 미답변(접수/확인 중) 건수. 백오피스 메뉴에 배지로 표시한다. */
    @Transactional(readOnly = true)
    fun countPending(): Long = inquiryRepository.countByStatusInAndIsDeletedFalse(PENDING_STATUSES)

    /**
     * 답변 저장. 상태를 지정하지 않으면 '답변 완료'로 올린다.
     * 이미 답변이 있으면 덮어쓰고 답변자/답변일시도 갱신한다.
     */
    @Transactional
    fun answer(id: Long, adminUserId: String, request: InquiryAnswerRequest): InquiryDto {
        val inquiry = findInquiry(id)

        val answer = request.answer.trim()
        if (answer.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "답변 내용을 입력해주세요.")
        }
        if (answer.length > MAX_ANSWER_LENGTH) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "답변은 ${MAX_ANSWER_LENGTH}자 이하여야 합니다.")
        }

        inquiry.answer = answer
        inquiry.answeredBy = adminUserId
        inquiry.answeredDts = LocalDateTime.now()
        inquiry.status = request.status ?: InquiryStatus.ANSWERED

        val saved = inquiryRepository.save(inquiry)
        notifyGuest(saved)
        return InquiryDto.from(saved)
    }

    /**
     * 비회원이 접수하면서 메일 주소를 남겼다면 답변을 메일로도 알려준다.
     * (회원은 앱의 "내 요청 내역"에서 바로 볼 수 있어서 따로 보내지 않는다.)
     *
     * 메일이 실패해도 답변 자체는 이미 저장됐으므로 로그만 남기고 넘어간다.
     */
    private fun notifyGuest(inquiry: InquiryMst) {
        val email = inquiry.guestEmail?.takeIf { it.isNotBlank() } ?: return
        try {
            val message = SimpleMailMessage()
            message.setTo(email)
            message.setSubject("[같이타] 보내주신 ${inquiry.type.label}에 답변이 등록되었습니다.")
            message.setText(
                """
                안녕하세요. 같이타입니다.

                보내주신 요청에 답변이 등록되었습니다.

                [${inquiry.type.label}] ${inquiry.title}

                ${inquiry.answer}

                앱의 '개선/버그 요청' 화면에서도 다시 확인하실 수 있습니다.
                """.trimIndent()
            )
            javaMailSender.send(message)
        } catch (e: Exception) {
            log.warn(e) { "문의 답변 메일 발송 실패 (inquiryId=${inquiry.id})" }
        }
    }

    /** 답변 없이 상태만 변경 (예: 접수 -> 확인 중) */
    @Transactional
    fun changeStatus(id: Long, status: InquiryStatus): InquiryDto {
        val inquiry = findInquiry(id)
        inquiry.status = status
        return InquiryDto.from(inquiryRepository.save(inquiry))
    }

    /**
     * 문의 숨김/복원. 게시물과 마찬가지로 소프트 삭제라 되돌릴 수 있다.
     * 숨기면 작성자의 "내 문의 내역"에서도 사라진다.
     */
    @Transactional
    fun setDeleted(id: Long, deleted: Boolean): InquiryDto {
        val inquiry = findInquiry(id)
        inquiry.isDeleted = deleted
        return InquiryDto.from(inquiryRepository.save(inquiry))
    }

    private fun findInquiry(id: Long): InquiryMst = inquiryRepository.findById(id)
        .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "문의를 찾을 수 없습니다: $id") }
}
