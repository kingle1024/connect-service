package com.connect.service.board.controller

import com.connect.service.board.dto.BoardCreateRequest
import com.connect.service.board.dto.PaginatedBoardResponse
import com.connect.service.board.service.BoardService
import com.connect.service.board.service.BoardParticipantService
import com.connect.service.board.dto.BoardParticipantDto
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
class BoardController(
    private val boardService: BoardService,
    private val participantService: BoardParticipantService,
) {

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
    ): ResponseEntity<Any> {
        // 회원은 로그인 토큰, 비회원은 게스트 쿠키로 인증된다. 둘 다 없으면 작성자를 알 수 없다.
        // (예전에는 요청 본문의 userId 를 그대로 믿어서 다른 사람 이름으로 글을 쓸 수 있었다)
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf(
                "status" to HttpStatus.UNAUTHORIZED.value(),
                "message" to "작성자를 확인할 수 없습니다. 다시 시도해주세요.",
                "code" to "AUTHENTICATION_REQUIRED"
            ))
        }
        return ResponseEntity.ok(boardService.createBoard(request, authentication.name))
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

    // 로그인(또는 게스트 쿠키) 상태면 내가 참여했는지(joined)도 함께 내려준다
    @GetMapping("/{id}")
    fun getBoardDetail(
        @PathVariable id: Long,
        authentication: Authentication?
    ): ResponseEntity<com.connect.service.board.dto.BoardResponseDto> {
        val detail = boardService.getBoardDetail(id)
            ?: return ResponseEntity.notFound().build()
        val joined = participantService.isJoined(id, authentication?.name)
        return ResponseEntity.ok(detail.copy(joined = joined))
    }

    // 모집 참여. 회원은 로그인 토큰, 비회원은 게스트 쿠키로 참여자를 확인한다.
    @PostMapping("/{id}/participants")
    fun joinBoard(@PathVariable id: Long, authentication: Authentication?): ResponseEntity<Any> {
        if (authentication == null) return actorRequired()
        return ResponseEntity.ok(participantService.join(id, authentication.name))
    }

    // 참여 취소
    @DeleteMapping("/{id}/participants")
    fun leaveBoard(@PathVariable id: Long, authentication: Authentication?): ResponseEntity<Any> {
        if (authentication == null) return actorRequired()
        return ResponseEntity.ok(participantService.leave(id, authentication.name))
    }

    // 참여자 목록 (작성자 제외, 참여 순)
    @GetMapping("/{id}/participants")
    fun getParticipants(@PathVariable id: Long): ResponseEntity<List<BoardParticipantDto>> {
        return ResponseEntity.ok(participantService.participants(id))
    }

    private fun actorRequired(): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf(
            "status" to HttpStatus.UNAUTHORIZED.value(),
            "message" to "참여자를 확인할 수 없습니다. 다시 시도해주세요.",
            "code" to "AUTHENTICATION_REQUIRED"
        ))

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
