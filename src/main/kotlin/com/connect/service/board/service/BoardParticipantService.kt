package com.connect.service.board.service

import com.connect.service.board.dto.BoardParticipantDto
import com.connect.service.board.dto.BoardResponseDto
import com.connect.service.board.entity.BoardMst
import com.connect.service.board.entity.BoardParticipant
import com.connect.service.board.repository.BoardParticipantRepository
import com.connect.service.board.repository.BoardRepository
import com.connect.service.user.domain.UserRole
import com.connect.service.user.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime

/**
 * 모집 참여/취소.
 *
 * board_mst.current_participants 는 작성자 1명을 포함한 인원이고, board_participant 한 줄이 참여자 1명이다.
 * 정원 확인과 인원 증감은 글 행을 잠근 상태(findByIdForUpdate)에서 해서 동시에 눌러도 정원을 넘지 않는다.
 */
@Service
class BoardParticipantService(
    private val boardRepository: BoardRepository,
    private val participantRepository: BoardParticipantRepository,
    private val userRepository: UserRepository,
    private val boardChatService: BoardChatService,
) {

    @Transactional
    fun join(boardId: Long, userId: String): BoardResponseDto {
        val user = userRepository.findByUserId(userId)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "참여자를 확인할 수 없습니다.")
        val board = lockActiveBoard(boardId)

        if (board.userId == userId) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "내가 만든 모집에는 이미 참여되어 있어요.")
        }
        if (participantRepository.existsByBoardIdAndUserId(boardId, userId)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "이미 참여한 모집이에요.")
        }
        if (!board.deadlineDts.isAfter(LocalDateTime.now())) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "마감일이 지난 모집이에요.")
        }
        if (board.currentParticipants >= board.maxCapacity) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "모집 인원이 다 찼어요.")
        }

        participantRepository.save(BoardParticipant(boardId = boardId, userId = userId, userName = user.name))
        board.currentParticipants += 1
        // 참여하면 모집 채팅방에도 들어간다
        boardChatService.addMember(board, userId)
        return toDto(boardRepository.save(board), joined = true)
    }

    @Transactional
    fun leave(boardId: Long, userId: String): BoardResponseDto {
        val board = lockActiveBoard(boardId)
        val participant = participantRepository.findByBoardIdAndUserId(boardId, userId)
            ?: throw ResponseStatusException(HttpStatus.CONFLICT, "참여하지 않은 모집이에요.")

        participantRepository.delete(participant)
        // 참여를 취소하면 모집 채팅방에서도 빠진다
        boardChatService.removeMember(board, userId)
        // 작성자 1명은 항상 남는다
        board.currentParticipants = (board.currentParticipants - 1).coerceAtLeast(1)
        return toDto(boardRepository.save(board), joined = false)
    }

    @Transactional(readOnly = true)
    fun participants(boardId: Long): List<BoardParticipantDto> =
        participantRepository.findAllByBoardIdOrderByIdAsc(boardId).map(BoardParticipantDto::from)

    @Transactional(readOnly = true)
    fun isJoined(boardId: Long, userId: String?): Boolean =
        userId != null && participantRepository.existsByBoardIdAndUserId(boardId, userId)

    private fun lockActiveBoard(boardId: Long): BoardMst {
        val board = boardRepository.findByIdForUpdate(boardId)
        if (board == null || board.isDeleted) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다.")
        }
        return board
    }

    private fun toDto(board: BoardMst, joined: Boolean): BoardResponseDto {
        val verified = userRepository.findByUserId(board.userId)
            ?.roles?.contains(UserRole.ROLE_VERIFIED) == true
        return BoardResponseDto.from(board, verified, joined)
    }
}
