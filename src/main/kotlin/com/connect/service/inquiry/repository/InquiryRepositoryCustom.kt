package com.connect.service.inquiry.repository

import com.connect.service.inquiry.entity.InquiryMst
import com.connect.service.inquiry.entity.InquiryStatus
import com.connect.service.inquiry.entity.InquiryType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface InquiryRepositoryCustom {
    /**
     * 백오피스 문의 검색. 조건은 모두 선택이며 null 이면 해당 조건을 걸지 않는다.
     *
     * @param keyword 제목/내용/작성자ID/작성자명 부분 일치
     * @param isDeleted null 이면 숨김 여부와 무관하게 전체 조회
     */
    fun searchForAdmin(
        keyword: String?,
        type: InquiryType?,
        status: InquiryStatus?,
        isDeleted: Boolean?,
        pageable: Pageable
    ): Page<InquiryMst>
}
