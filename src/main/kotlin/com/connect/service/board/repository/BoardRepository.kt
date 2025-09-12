package com.connect.service.board.repository

import com.connect.service.board.entity.BoardMst
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BoardRepository : JpaRepository<BoardMst, Long> {

}
