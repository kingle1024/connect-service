package com.connect.service.board

import org.springframework.web.bind.annotation.* // <- 임포트 확인!

data class CreateBoardRequest(
    val title: String,
    val content: String,
    val author: String
)

@RestController
@RequestMapping("/api/boards") // URL 경로도 /api/boards로 변경!
class BoardController(private val boardService: BoardService) { // 필드 이름도 boardService로 변경!

    @GetMapping
    fun getBoards(): List<BoardMst> { // 함수 이름과 반환 타입 변경!
        return boardService.getAllBoards() // 서비스에서 모든 게시글을 가져와서 반환
    }

    @PostMapping
    fun createBoard(@RequestBody request: CreateBoardRequest): BoardMst { // 함수 이름과 매개변수, 반환 타입 변경!
        return boardService.createBoard(request.title, request.content, request.author) // 서비스에 게시글 생성을 요청하고 생성된 Board 객체를 반환
    }
}
