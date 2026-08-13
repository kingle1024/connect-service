package com.connect.service.inquiry.repository.impl

import com.connect.service.inquiry.entity.InquiryMst
import com.connect.service.inquiry.entity.InquiryStatus
import com.connect.service.inquiry.entity.InquiryType
import com.connect.service.inquiry.entity.QInquiryMst
import com.connect.service.inquiry.repository.InquiryRepositoryCustom
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class InquiryRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : InquiryRepositoryCustom {

    private val inquiry = QInquiryMst.inquiryMst

    override fun searchForAdmin(
        keyword: String?,
        type: InquiryType?,
        status: InquiryStatus?,
        isDeleted: Boolean?,
        pageable: Pageable
    ): Page<InquiryMst> {
        val where = BooleanBuilder()

        keyword?.trim()?.takeIf { it.isNotEmpty() }?.let { kw ->
            where.and(
                inquiry.title.containsIgnoreCase(kw)
                    .or(inquiry.content.containsIgnoreCase(kw))
                    .or(inquiry.userId.containsIgnoreCase(kw))
                    .or(inquiry.userName.containsIgnoreCase(kw))
                    // 비회원 문의는 userId 가 없어서 남긴 메일 주소로도 찾을 수 있게 한다.
                    .or(inquiry.guestEmail.containsIgnoreCase(kw))
            )
        }
        type?.let { where.and(inquiry.type.eq(it)) }
        status?.let { where.and(inquiry.status.eq(it)) }
        isDeleted?.let { where.and(inquiry.isDeleted.eq(it)) }

        val rows = queryFactory
            .selectFrom(inquiry)
            .where(where)
            .orderBy(inquiry.id.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory
            .select(inquiry.count())
            .from(inquiry)
            .where(where)
            .fetchOne() ?: 0L

        return PageImpl(rows, pageable, total)
    }
}
