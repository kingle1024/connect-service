package com.connect.service.board

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BoardService(private val boardRepository: BoardRepository) {

    fun getAllBoards(): List<BoardMst> {
        return boardRepository.findAll()
    }

    @Transactional
    fun createBoard(title: String, content: String, author: String): BoardMst {
        val newBoardMst = BoardMst(title = title, content = content, author = author)
        return boardRepository.save(newBoardMst)
    }
}
