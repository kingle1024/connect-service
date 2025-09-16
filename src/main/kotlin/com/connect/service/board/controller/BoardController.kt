package com.connect.service.board.controller

import com.connect.service.board.dto.BoardCreateRequest
import com.connect.service.board.service.BoardService
import com.connect.service.board.dto.UpdateBoardRequest
import com.connect.service.board.entity.BoardMst
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.* // <- 임포트 확인!

@RestController
@RequestMapping("/api/boards")
@CrossOrigin
class BoardController(private val boardService: BoardService) {

    @GetMapping
    fun getBoards(): List<BoardMst> {
        return boardService.getAllBoards()
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
