package com.connect.service.board

import org.springframework.web.bind.annotation.* // <- 임포트 확인!

data class CreateBoardRequest(
    val title: String,
    val content: String,
    val author: String
)

@RestController
@RequestMapping("/api/boards")
class BoardController(private val boardService: BoardService) {

    @GetMapping
    fun getBoards(): List<BoardMst> {
        return boardService.getAllBoards()
    }

    @PostMapping
    fun createBoard(@RequestBody request: CreateBoardRequest): BoardMst {
        return boardService.createBoard(request.title, request.content, request.author)
    }
}
