package com.connect.service.comment

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CommentMstRepository : JpaRepository<CommentMst, Long> {
    fun findAllByPostIdOrderByInsertDtsAsc(postId: Long): List<CommentMst>
}
