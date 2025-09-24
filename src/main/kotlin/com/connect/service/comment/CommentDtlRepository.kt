package com.connect.service.comment

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CommentDtlRepository : JpaRepository<CommentDtl, Long> {
    fun findAllByParentIdOrderByInsertDtsAsc(parentId: Long): List<CommentDtl>
    fun countByParentIdAndIsDeletedFalse(parentId: Long): Long // 특정 부모의 삭제되지 않은 대댓글 수 세기
}
