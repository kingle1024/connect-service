package com.connect.service.board

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional // <- 임포트 확인!

@Service
class BoardService(private val boardRepository: BoardRepository) { // 필드 이름도 boardRepository로 변경!

    fun getAllBoards(): List<BoardMst> {
        return boardRepository.findAll()
    }

    @Transactional
    fun createBoard(title: String, content: String, author: String): BoardMst {
        val newBoardMst = BoardMst(title = title, content = content, author = author)
        return boardRepository.save(newBoardMst)
    }
}
