package com.connect.service.admin.service

import com.connect.service.admin.dto.AdminBoardDto
import com.connect.service.admin.dto.AdminBoardUpdateRequest
import com.connect.service.admin.dto.PageResponse
import com.connect.service.board.repository.BoardRepository
import com.connect.service.category.service.CategoryService
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

/**
 * 백오피스 게시물 관리.
 *
 * BoardService 와 달리 작성자 본인 여부를 확인하지 않는다.
 * 접근 통제는 SecurityConfig 의 관리자 경로 규칙(hasRole("ADMIN"))이 담당한다.
 */
@Service
class AdminBoardService(
    private val boardRepository: BoardRepository,
    private val categoryService: CategoryService
) {

    @Transactional(readOnly = true)
    fun search(
        keyword: String?,
        category: String?,
        isDeleted: Boolean?,
        pageable: Pageable
    ): PageResponse<AdminBoardDto> {
        val page = boardRepository.searchForAdmin(keyword, category, isDeleted, pageable)
        return PageResponse.from(page) { AdminBoardDto.from(it) }
    }

    @Transactional(readOnly = true)
    fun get(id: Long): AdminBoardDto = AdminBoardDto.from(findBoard(id))

    @Transactional
    fun update(id: Long, request: AdminBoardUpdateRequest): AdminBoardDto {
        val board = findBoard(id)

        if (request.title.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "제목을 입력해주세요.")
        }
        if (request.content.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "내용을 입력해주세요.")
        }
        if (request.destination.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "도착지를 입력해주세요.")
        }

        // 백오피스에서는 등록된 카테고리 중에서만 고를 수 있도록 한다 (화면도 select 로 제공).
        val category = request.category.trim()
        if (category.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "카테고리를 선택해주세요.")
        }
        if (category != board.category && category !in categoryService.getActiveCategoryNames()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "등록되지 않은 카테고리입니다: $category")
        }

        // 이미 참여한 인원보다 적게 줄일 수는 없다 (BoardService.updateBoard 와 동일한 규칙)
        if (request.maxCapacity < board.currentParticipants) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "최대 모집 인원은 현재 참여 인원(${board.currentParticipants}명)보다 적을 수 없습니다."
            )
        }

        board.title = request.title.trim()
        board.content = request.content.trim()
        board.category = category
        board.destination = request.destination.trim()
        board.maxCapacity = request.maxCapacity
        board.deadlineDts = request.deadlineDts

        return AdminBoardDto.from(boardRepository.save(board))
    }

    /**
     * 게시물 숨김/복원. 사용자 API 와 같은 소프트 삭제(is_deleted)를 쓰므로 되돌릴 수 있다.
     */
    @Transactional
    fun setDeleted(id: Long, deleted: Boolean): AdminBoardDto {
        val board = findBoard(id)
        board.isDeleted = deleted
        return AdminBoardDto.from(boardRepository.save(board))
    }

    private fun findBoard(id: Long) = boardRepository.findById(id)
        .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다: $id") }
}
