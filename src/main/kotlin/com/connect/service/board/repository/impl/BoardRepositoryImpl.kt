package com.connect.service.board.repository.impl

import com.connect.service.board.entity.BoardMst
import com.connect.service.board.entity.QBoardMst
import com.connect.service.board.repository.BoardRepositoryCustom
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class BoardRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : BoardRepositoryCustom {

    private val board = QBoardMst.boardMst

    override fun searchForAdmin(
        keyword: String?,
        category: String?,
        isDeleted: Boolean?,
        pageable: Pageable
    ): Page<BoardMst> {
        val where = BooleanBuilder()

        keyword?.trim()?.takeIf { it.isNotEmpty() }?.let { kw ->
            where.and(
                board.title.containsIgnoreCase(kw)
                    .or(board.content.containsIgnoreCase(kw))
                    .or(board.userId.containsIgnoreCase(kw))
                    .or(board.userName.containsIgnoreCase(kw))
            )
        }
        category?.trim()?.takeIf { it.isNotEmpty() }?.let { where.and(board.category.eq(it)) }
        isDeleted?.let { where.and(board.isDeleted.eq(it)) }

        val content = queryFactory
            .selectFrom(board)
            .where(where)
            .orderBy(board.insertDts.desc(), board.id.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory
            .select(board.count())
            .from(board)
            .where(where)
            .fetchOne() ?: 0L

        return PageImpl(content, pageable, total)
    }

    override fun searchBoards(
        category: String?,
        chipCategories: List<String>?,
        destination: String?,
        title: String?,
        content: String?,
        openOnly: Boolean,
        pageable: Pageable
    ): Page<BoardMst> {
        val where = BooleanBuilder()
        where.and(board.isDeleted.isFalse)

        category?.trim()?.takeIf { it.isNotEmpty() }?.let { name ->
            if (chipCategories.isNullOrEmpty()) {
                where.and(board.category.eq(name))
            } else {
                // '기타' 칩: 이름이 정확히 같은 글 + 칩이 없는 카테고리(직접입력 값 등) 글까지.
                // notIn 은 NULL 을 걸러내므로 category 가 비어 있는 과거 데이터는 따로 포함시킨다.
                where.and(
                    board.category.eq(name)
                        .or(board.category.isNull)
                        .or(board.category.notIn(chipCategories))
                )
            }
        }
        destination?.trim()?.takeIf { it.isNotEmpty() }?.let { where.and(board.destination.containsIgnoreCase(it)) }
        title?.trim()?.takeIf { it.isNotEmpty() }?.let { where.and(board.title.containsIgnoreCase(it)) }
        content?.trim()?.takeIf { it.isNotEmpty() }?.let { where.and(board.content.containsIgnoreCase(it)) }

        if (openOnly) {
            // 모집 미완료: 정원이 아직 차지 않았고 마감 시각도 지나지 않은 글
            where.and(board.currentParticipants.lt(board.maxCapacity))
            where.and(board.deadlineDts.gt(LocalDateTime.now()))
        }

        val rows = queryFactory
            .selectFrom(board)
            .where(where)
            .orderBy(board.insertDts.desc(), board.id.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory
            .select(board.count())
            .from(board)
            .where(where)
            .fetchOne() ?: 0L

        return PageImpl(rows, pageable, total)
    }
}
