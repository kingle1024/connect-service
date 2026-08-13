package com.connect.service.inquiry.entity

/**
 * 문의 처리 상태. 관리자가 백오피스에서 변경한다.
 * 답변을 저장하면 서비스가 자동으로 ANSWERED 로 올린다(InquiryAdminService.answer 참고).
 */
enum class InquiryStatus(val label: String) {
    RECEIVED("접수"),
    IN_PROGRESS("확인 중"),
    ANSWERED("답변 완료"),
    REJECTED("반영 불가")
}
