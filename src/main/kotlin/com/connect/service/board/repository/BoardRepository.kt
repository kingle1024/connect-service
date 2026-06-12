package com.connect.service.board.repository

import com.connect.service.board.entity.BoardMst
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

@Repository
interface BoardRepository : JpaRepository<BoardMst, Long> {
    fun findAllByIsDeletedFalse(pageable: Pageable): Page<BoardMst>

    // 특정 작성자(userId)의 삭제되지 않은 게시글 목록 (마이페이지 - 내가 쓴 글)
    fun findAllByUserIdAndIsDeletedFalse(userId: String, pageable: Pageable): Page<BoardMst>

    // 작성자(userId)의 게시글 작성자명을 일괄 변경 (이름 변경 시 기존 글도 함께 반영)
    @Modifying
    @Query("UPDATE BoardMst b SET b.userName = :userName WHERE b.userId = :userId")
    fun updateUserNameByUserId(@Param("userId") userId: String, @Param("userName") userName: String): Int
}
