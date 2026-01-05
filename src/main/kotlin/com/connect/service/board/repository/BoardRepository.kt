package com.connect.service.board.repository

import com.connect.service.board.entity.BoardMst
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

@Repository
interface BoardRepository : JpaRepository<BoardMst, Long> {
    fun findAllByIsDeletedFalse(pageable: Pageable): Page<BoardMst>
}
