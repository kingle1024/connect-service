package com.connect.service.board.repository

import com.connect.service.board.entity.BoardParticipant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BoardParticipantRepository : JpaRepository<BoardParticipant, Long> {
    fun existsByBoardIdAndUserId(boardId: Long, userId: String): Boolean

    fun findByBoardIdAndUserId(boardId: Long, userId: String): BoardParticipant?

    fun findAllByBoardIdOrderByIdAsc(boardId: Long): List<BoardParticipant>
}
