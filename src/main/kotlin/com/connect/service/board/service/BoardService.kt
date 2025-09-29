package com.connect.service.board.service

import com.connect.service.board.dto.BoardCreateRequest
import com.connect.service.board.dto.BoardResponseDto
import com.connect.service.board.dto.PaginatedBoardResponse
import com.connect.service.board.entity.BoardMst
import com.connect.service.board.repository.BoardRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

@Service
class BoardService(private val boardRepository: BoardRepository) {

    @Transactional(readOnly = true)
    fun getAllBoards(pageable: Pageable): PaginatedBoardResponse {
        val boardPage: Page<BoardMst> = boardRepository.findAll(pageable)

        val boardResponseDtos: List<BoardResponseDto> = boardPage.content
            .map { BoardResponseDto.from(it) } // 각 BoardMst 엔티티를 DTO로 변환

        // 다음 페이지가 있다면 다음 페이지 번호를, 없으면 null 반환
        val nextPageToken: Int? = if (boardPage.hasNext()) {
            boardPage.number + 1
        } else {
            null
        }

        return PaginatedBoardResponse(
            nextPageToken = nextPageToken,
            posts = boardResponseDtos
        )
    }

    @Transactional
    fun createBoard(request: BoardCreateRequest): BoardMst {
        val newBoardMst = BoardMst(
            title = request.title,
            content = request.content,
            category = request.category,
            userId = request.userId,
            userName = request.userName,
            deadlineDts = request.deadlineDts,
            destination = request.destination,
            maxCapacity = request.maxCapacity,
            currentParticipants = request.currentParticipants
        )
        return boardRepository.save(newBoardMst)
    }


    @Transactional
    fun updateBoard(id: Long, title: String, content: String, userId: String?): BoardMst {
        val existingBoard = boardRepository.findById(id)
            .orElseThrow { NoSuchElementException("Board with ID $id not found") }

        // 2. 게시글의 내용을 업데이트합니다.
        existingBoard.title = title
        existingBoard.content = content
        // author는 변경되지 않을 수도 있으므로 null 체크 후 업데이트
        if (userId != null) {
            existingBoard.userId = userId
        }

        // 3. 업데이트된 게시글을 저장하고 반환합니다. (JPA는 변경 감지를 통해 자동으로 업데이트합니다)
        return boardRepository.save(existingBoard)
    }

    fun getBoardById(id: Long): Optional<BoardMst> {
            return boardRepository.findById(id)
        }

}
