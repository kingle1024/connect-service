package com.connect.service.comment

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository // 스프링 빈으로 등록
interface CommentRepository : JpaRepository<CommentMst, Long> {
    // 특정 게시글의 모든 댓글을 생성 시간 순으로 조회 (부모 댓글 포함)
    fun findAllByBoardIdOrderByInsertDtsAsc(boardId: Long): List<CommentMst>

    // 특정 게시글의 최상위 댓글 (parentCommentId가 null)만 조회
    fun findAllByBoardIdAndParentCommentIdIsNullOrderByInsertDtsAsc(boardId: Long): List<CommentMst>

    // 특정 부모 댓글 ID에 해당하는 대댓글들만 조회
    fun findAllByParentCommentIdOrderByInsertDtsAsc(parentCommentId: Long): List<CommentMst>
}
