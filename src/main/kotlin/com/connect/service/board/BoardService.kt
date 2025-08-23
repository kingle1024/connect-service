package com.connect.service.board

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional // <- 임포트 확인!

@Service
class BoardService(private val boardRepository: BoardRepository) { // 필드 이름도 boardRepository로 변경!

    fun getAllBoards(): List<Board> {
        return boardRepository.findAll()
    }

    @Transactional
    fun createBoard(content: String): Board {
        val newBoard = Board(content = content)
        return boardRepository.save(newBoard)
    }
}
