package com.connect.service.board.service

import com.connect.service.board.dto.BoardCreateRequest
import com.connect.service.board.dto.BoardResponseDto
import com.connect.service.board.dto.PaginatedBoardResponse
import com.connect.service.board.dto.UpdateBoardRequest
import com.connect.service.board.entity.BoardMst
import com.connect.service.board.repository.BoardRepository
import com.connect.service.category.service.CategoryService
import com.connect.service.user.domain.UserRole
import com.connect.service.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

@Service
class BoardService(
    private val boardRepository: BoardRepository,
    private val userRepository: UserRepository,
    private val categoryService: CategoryService
) {

    // 주어진 작성자 ID들 중 더존 이메일 인증(ROLE_VERIFIED)된 사용자 ID 집합을 반환
    private fun verifiedUserIds(userIds: Collection<String>): Set<String> {
        val distinct = userIds.distinct()
        if (distinct.isEmpty()) {
            return emptySet()
        }
        return userRepository.findByUserIdIn(distinct)
            .filter { it.roles.contains(UserRole.ROLE_VERIFIED) }
            .map { it.userId }
            .toSet()
    }

    @Transactional(readOnly = true)
    fun getAllBoards(
        pageable: Pageable,
        category: String? = null,
        destination: String? = null,
        title: String? = null,
        content: String? = null,
        openOnly: Boolean = false
    ): PaginatedBoardResponse {
        val categoryFilter = category?.trim()?.takeIf { it.isNotEmpty() }
        // '기타' 칩은 전용 칩이 없는 카테고리의 글까지 함께 보여준다.
        // (직접입력으로 만든 카테고리, 관리자가 비활성화한 옛 카테고리 등은
        //  칩이 없어서 그대로 두면 '전체'에서만 볼 수 있다)
        val chipCategories = categoryFilter
            ?.takeIf { categoryService.isEtcCategory(it) }
            ?.let { categoryService.getActiveCategoryNames() }

        // 조건이 없으면(모두 null/빈 값) 전체 조회와 동일하게 동작한다
        val boardPage: Page<BoardMst> = boardRepository.searchBoards(
            categoryFilter, chipCategories, destination, title, content, openOnly, pageable
        )

        val verified = verifiedUserIds(boardPage.content.map { it.userId })
        val boardResponseDtos: List<BoardResponseDto> = boardPage.content
            .map { BoardResponseDto.from(it, verified.contains(it.userId)) }

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

    // 모집 탭 카테고리 필터에 노출할 카테고리 목록 (관리자가 백오피스에서 관리하는 활성 카테고리)
    @Transactional(readOnly = true)
    fun getCategories(): List<String> {
        return categoryService.getActiveCategoryNames()
    }

    // 특정 작성자의 게시글 목록 (마이페이지 - 내가 쓴 글)
    @Transactional(readOnly = true)
    fun getBoardsByAuthor(userId: String, pageable: Pageable): PaginatedBoardResponse {
        val boardPage: Page<BoardMst> = boardRepository.findAllByUserIdAndIsDeletedFalse(userId, pageable)

        val verified = verifiedUserIds(boardPage.content.map { it.userId })
        val boardResponseDtos: List<BoardResponseDto> = boardPage.content
            .map { BoardResponseDto.from(it, verified.contains(it.userId)) }

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

    // 게시글 상세 (작성자 인증 여부 verified 포함)
    @Transactional(readOnly = true)
    fun getBoardDetail(id: Long): BoardResponseDto? {
        val board = boardRepository.findById(id).orElse(null) ?: return null
        val verified = verifiedUserIds(listOf(board.userId)).contains(board.userId)
        return BoardResponseDto.from(board, verified)
    }

    @Transactional
    fun createBoard(request: BoardCreateRequest, requestUserId: String? = null): BoardMst {
        // 로그인 상태라면 작성자 정보는 토큰 기준으로 채운다. 요청 본문 값으로 위조할 수 없다.
        val author = requestUserId?.let { userRepository.findByUserId(it) }
        val newBoardMst = BoardMst(
            title = request.title,
            content = request.content,
            category = request.category,
            userId = author?.userId ?: request.userId,
            userName = author?.name ?: request.userName,
            deadlineDts = request.deadlineDts,
            destination = request.destination,
            maxCapacity = request.maxCapacity,
            currentParticipants = request.currentParticipants
        )
        return boardRepository.save(newBoardMst)
    }


    @Transactional
    fun updateBoard(request: UpdateBoardRequest, requestUserId: String): BoardResponseDto {
        val existingBoard = boardRepository.findById(request.id)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다: ${request.id}")
            }

        if (existingBoard.isDeleted) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다: ${request.id}")
        }

        // 본인 게시물인지 확인 (deleteBoard와 동일한 규칙)
        if (existingBoard.userId != requestUserId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "게시글을 수정할 권한이 없습니다.")
        }

        if (request.title.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "제목을 입력해주세요.")
        }
        if (request.content.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "내용을 입력해주세요.")
        }
        if (request.destination.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "도착지를 입력해주세요.")
        }
        if (request.category.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "카테고리를 입력해주세요.")
        }
        // 이미 참여한 인원보다 적게 줄일 수는 없다
        if (request.maxCapacity < existingBoard.currentParticipants) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "최대 모집 인원은 현재 참여 인원(${existingBoard.currentParticipants}명)보다 적을 수 없습니다."
            )
        }

        existingBoard.title = request.title.trim()
        existingBoard.content = request.content.trim()
        existingBoard.category = request.category.trim()
        existingBoard.destination = request.destination.trim()
        existingBoard.maxCapacity = request.maxCapacity
        existingBoard.deadlineDts = request.deadlineDts

        val saved = boardRepository.save(existingBoard)
        val verified = verifiedUserIds(listOf(saved.userId)).contains(saved.userId)
        return BoardResponseDto.from(saved, verified)
    }

    @Transactional
   fun deleteBoard(boardId: Long, requestUserId: String) {
       val board = boardRepository.findById(boardId)
           .orElseThrow {
               ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다: $boardId")
           }

       if (board.isDeleted) {
           return
       }

       // 본인 게시물인지 확인
       if (board.userId != requestUserId) {
           throw ResponseStatusException(HttpStatus.FORBIDDEN, "게시글을 삭제할 권한이 없습니다.")
       }

       // 소프트 삭제 (isDeleted 플래그를 true로 변경)
       board.isDeleted = true
       boardRepository.save(board) // 변경사항 저장
   }

    fun getBoardById(id: Long): Optional<BoardMst> {
            return boardRepository.findById(id)
        }

}
