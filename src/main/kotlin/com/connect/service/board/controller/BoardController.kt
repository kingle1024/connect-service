package com.connect.service.board.controller

import com.connect.service.board.dto.BoardCreateRequest
import com.connect.service.board.dto.PaginatedBoardResponse
import com.connect.service.board.service.BoardService
import com.connect.service.board.dto.UpdateBoardRequest
import com.connect.service.board.entity.BoardMst
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.* // <- 임포트 확인!
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

@RestController
@RequestMapping("/api/boards")
@CrossOrigin
class BoardController(private val boardService: BoardService) {

    @GetMapping
    fun getBoards(
        @PageableDefault(size = 10, sort = ["insertDts"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<PaginatedBoardResponse> {
        val paginatedResponse: PaginatedBoardResponse = boardService.getAllBoards(pageable)

        return ResponseEntity.ok(paginatedResponse)
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
}
