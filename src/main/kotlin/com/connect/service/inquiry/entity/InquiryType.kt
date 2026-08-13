package com.connect.service.inquiry.entity

/**
 * 문의 종류. 앱의 "개선/버그 요청" 화면에서 사용자가 고른다.
 * 이름(name)이 DB에 그대로 저장되므로 값을 바꾸면 기존 데이터도 함께 손봐야 한다.
 */
enum class InquiryType(val label: String) {
    IMPROVEMENT("개선 요청"),
    BUG("버그 신고"),
    ETC("기타 문의")
}
