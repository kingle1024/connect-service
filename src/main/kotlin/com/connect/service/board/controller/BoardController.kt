package com.connect.service.board.controller

import com.connect.service.board.dto.BoardCreateRequest
import com.connect.service.board.dto.PaginatedBoardResponse
import com.connect.service.board.service.BoardService
import com.connect.service.board.dto.UpdateBoardRequest
import com.connect.service.board.entity.BoardMst
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication

@RestController
@RequestMapping("/api/boards")
class BoardController(private val boardService: BoardService) {

    @GetMapping
    fun getBoards(
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) destination: String?,
        @RequestParam(required = false) title: String?,
        @RequestParam(required = false) content: String?,
        // 모집 미완료(정원 미달 + 마감 전)인 글만 조회
        @RequestParam(required = false, defaultValue = "false") openOnly: Boolean,
        @PageableDefault(size = 10, sort = ["insertDts"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<PaginatedBoardResponse> {
        val paginatedResponse: PaginatedBoardResponse =
            boardService.getAllBoards(pageable, category, destination, title, content, openOnly)

        return ResponseEntity.ok(paginatedResponse)
    }

    // 카테고리 필터 칩 목록. /{id} 보다 구체적인 경로이므로 먼저 매칭된다.
    @GetMapping("/categories")
    fun getCategories(): ResponseEntity<List<String>> {
        return ResponseEntity.ok(boardService.getCategories())
    }

    // 특정 작성자(userId)가 쓴 게시글 목록 (마이페이지 - 내가 쓴 글)
    // 경로 세그먼트 수가 달라 GET /api/boards/{id} 와 충돌하지 않음
    @GetMapping("/author/{userId}")
    fun getBoardsByAuthor(
        @PathVariable userId: String,
        @PageableDefault(size = 10, sort = ["insertDts"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<PaginatedBoardResponse> {
        return ResponseEntity.ok(boardService.getBoardsByAuthor(userId, pageable))
    }

    @PostMapping
    fun createBoard(
        @RequestBody request: BoardCreateRequest,
        authentication: Authentication?
    ): BoardMst {
        return boardService.createBoard(request, authentication?.name)
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}")
    fun updateBoard(
        @PathVariable id: Long,
        @RequestBody request: UpdateBoardRequest,
        authentication: Authentication?
    ): ResponseEntity<Any> {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf(
                "status" to HttpStatus.UNAUTHORIZED.value(),
                "message" to "로그인이 필요합니다. 토큰이 없거나 만료되었습니다.",
                "code" to "AUTHENTICATION_REQUIRED"
            ))
        }
        if (id != request.id) {
            throw IllegalArgumentException("ID in path ($id) must match ID in request body (${request.id})")
        }
        // 작성자 본인만 수정 가능. 작성자는 토큰에서 가져오며 요청 본문으로 바꿀 수 없다.
        return ResponseEntity.ok(boardService.updateBoard(request, authentication.name))
    }

    @GetMapping("/{id}")
    fun getBoardDetail(@PathVariable id: Long): ResponseEntity<com.connect.service.board.dto.BoardResponseDto> {
        val detail = boardService.getBoardDetail(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(detail)
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    fun deleteBoard(
        @PathVariable id: Long,
        authentication: Authentication?
    ): ResponseEntity<Any> {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf(
                "status" to HttpStatus.UNAUTHORIZED.value(),
                "message" to "로그인이 필요합니다. 토큰이 없거나 만료되었습니다.",
                "code" to "AUTHENTICATION_REQUIRED"
            ))
        }
        boardService.deleteBoard(id, authentication.name)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }
}
