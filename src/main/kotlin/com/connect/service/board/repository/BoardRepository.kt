package com.connect.service.board.repository

import com.connect.service.board.entity.BoardMst
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

@Repository
interface BoardRepository : JpaRepository<BoardMst, Long> {
    fun findAllByIsDeletedFalse(pageable: Pageable): Page<BoardMst>

    // 특정 작성자(userId)의 삭제되지 않은 게시글 목록 (마이페이지 - 내가 쓴 글)
    fun findAllByUserIdAndIsDeletedFalse(userId: String, pageable: Pageable): Page<BoardMst>
}
