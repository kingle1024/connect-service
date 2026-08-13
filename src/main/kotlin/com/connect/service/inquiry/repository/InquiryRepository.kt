package com.connect.service.inquiry.repository

import com.connect.service.inquiry.entity.InquiryMst
import com.connect.service.inquiry.entity.InquiryStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface InquiryRepository : JpaRepository<InquiryMst, Long>, InquiryRepositoryCustom {

    // 앱의 "내 문의 내역". 숨김 처리된 문의는 제외한다.
    fun findAllByUserIdAndIsDeletedFalseOrderByIdDesc(userId: String): List<InquiryMst>

    // 비회원의 "내 문의 내역". 단말에 캐시된 guestKey 로만 찾을 수 있다.
    fun findAllByGuestKeyAndIsDeletedFalseOrderByIdDesc(guestKey: String): List<InquiryMst>

    // 백오피스 사이드바에 표시할 미처리 건수
    fun countByStatusInAndIsDeletedFalse(statuses: Collection<InquiryStatus>): Long
}
