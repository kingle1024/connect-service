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
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:8082"], allowCredentials = "true")
class BoardController(private val boardService: BoardService) {

    @GetMapping
    fun getBoards(
        @PageableDefault(size = 10, sort = ["insertDts"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<PaginatedBoardResponse> {
        val paginatedResponse: PaginatedBoardResponse = boardService.getAllBoards(pageable)

        return ResponseEntity.ok(paginatedResponse)
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
    fun createBoard(@RequestBody request: BoardCreateRequest): BoardMst {
        return boardService.createBoard(request)
    }

    @PutMapping("/{id}")
    fun updateBoard(@PathVariable id: Long, @RequestBody request: UpdateBoardRequest): BoardMst {
        if (id != request.id) {
            throw IllegalArgumentException("ID in path ($id) must match ID in request body (${request.id})")
        }
        return boardService.updateBoard(request.id, request.title, request.content, request.author)
    }

    @GetMapping("/{id}")
    fun getBoardDetail(@PathVariable id: Long): ResponseEntity<BoardMst> {
        return boardService.getBoardById(id)
            .map { board -> ResponseEntity.ok(board) }
            .orElse(ResponseEntity.notFound().build())
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
