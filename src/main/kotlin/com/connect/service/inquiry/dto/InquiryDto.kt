package com.connect.service.inquiry.dto

import com.connect.service.inquiry.entity.InquiryMst
import com.connect.service.inquiry.entity.InquiryStatus
import com.connect.service.inquiry.entity.InquiryType
import java.time.LocalDateTime

/**
 * 문의 응답. 앱(내 문의 내역)과 백오피스(문의 관리)가 같은 형태를 쓴다.
 *
 * 라벨(typeLabel/statusLabel)을 서버에서 내려주므로 화면마다 한글 문구를 중복하지 않는다.
 */
data class InquiryDto(
    val id: Long,
    val type: InquiryType,
    val typeLabel: String,
    val title: String,
    val content: String,
    val status: InquiryStatus,
    val statusLabel: String,
    val answer: String?,
    val answeredBy: String?,
    val answeredDts: LocalDateTime?,
    // 비회원 문의면 null
    val userId: String?,
    val userName: String,
    // 로그인 없이 접수된 문의인지. 백오피스에서 작성자 표기를 나눌 때 쓴다.
    val guest: Boolean,
    // 비회원이 답변받을 주소로 남긴 메일. 안 남겼으면 null
    val guestEmail: String?,
    val insertDts: LocalDateTime?,
    val updateDts: LocalDateTime?,
    val deleted: Boolean
) {
    companion object {
        fun from(inquiry: InquiryMst): InquiryDto = InquiryDto(
            id = inquiry.id ?: throw IllegalStateException("문의 ID가 없습니다."),
            type = inquiry.type,
            typeLabel = inquiry.type.label,
            title = inquiry.title,
            content = inquiry.content,
            status = inquiry.status,
            statusLabel = inquiry.status.label,
            answer = inquiry.answer,
            answeredBy = inquiry.answeredBy,
            answeredDts = inquiry.answeredDts,
            userId = inquiry.userId,
            userName = inquiry.userName,
            guest = inquiry.userId == null,
            guestEmail = inquiry.guestEmail,
            insertDts = inquiry.insertDts,
            updateDts = inquiry.updateDts,
            deleted = inquiry.isDeleted
        )
    }
}
