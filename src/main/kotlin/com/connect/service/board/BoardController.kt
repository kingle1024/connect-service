package com.connect.service.board

import com.connect.service.board.dto.UpdateBoardRequest
import org.springframework.web.bind.annotation.* // <- 임포트 확인!

data class CreateBoardRequest(
    val title: String,
    val content: String,
    val author: String
)

@RestController
@RequestMapping("/api/boards")
@CrossOrigin
class BoardController(private val boardService: BoardService) {

    @GetMapping
    fun getBoards(): List<BoardMst> {
        return boardService.getAllBoards()
    }

    @PostMapping
    fun createBoard(@RequestBody request: CreateBoardRequest): BoardMst {
        return boardService.createBoard(request.title, request.content, request.author)
    }

    @PutMapping("/{id}")
    fun updateBoard(@PathVariable id: Long, @RequestBody request: UpdateBoardRequest): BoardMst {
        if (id != request.id) {
            throw IllegalArgumentException("ID in path ($id) must match ID in request body (${request.id})")
        }
        return boardService.updateBoard(request.id, request.title, request.content, request.author)
    }
}
