package com.connect.service.inquiry.dto

import com.connect.service.inquiry.entity.InquiryStatus
import com.connect.service.inquiry.entity.InquiryType

/**
 * 앱에서 문의를 접수할 때 보내는 본문.
 *
 * @param guestKey 비로그인 접수에만 사용. 단말이 만들어 캐시에 보관하는 키로, 나중에 내 문의 내역을 다시 찾는 열쇠다.
 * @param guestEmail 비로그인 접수의 선택 입력값. 넣어두면 관리자가 답변할 때 이 주소로 메일이 나간다.
 */
data class InquiryCreateRequest(
    val type: InquiryType = InquiryType.IMPROVEMENT,
    val title: String,
    val content: String,
    val guestKey: String? = null,
    val guestEmail: String? = null
)

/**
 * 백오피스 답변 저장.
 *
 * @param status 답변과 함께 상태도 바꾸고 싶을 때 지정한다. 비우면 서버가 ANSWERED 로 올린다.
 */
data class InquiryAnswerRequest(
    val answer: String,
    val status: InquiryStatus? = null
)

/** 백오피스에서 답변 없이 상태만 바꿀 때 사용. */
data class InquiryStatusRequest(
    val status: InquiryStatus
)
