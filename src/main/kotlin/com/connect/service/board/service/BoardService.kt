package com.connect.service.board.service

import com.connect.service.board.entity.BoardMst
import com.connect.service.board.repository.BoardRepository
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

    @Transactional
    fun updateBoard(id: Long, title: String, content: String, author: String?): BoardMst {
        val existingBoard = boardRepository.findById(id)
            .orElseThrow { NoSuchElementException("Board with ID $id not found") }

        // 2. 게시글의 내용을 업데이트합니다.
        existingBoard.title = title
        existingBoard.content = content
        // author는 변경되지 않을 수도 있으므로 null 체크 후 업데이트
        if (author != null) {
            existingBoard.author = author
        }

        // 3. 업데이트된 게시글을 저장하고 반환합니다. (JPA는 변경 감지를 통해 자동으로 업데이트합니다)
        return boardRepository.save(existingBoard)
    }
}
