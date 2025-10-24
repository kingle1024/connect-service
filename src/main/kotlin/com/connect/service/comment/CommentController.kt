package com.connect.service.comment

import com.connect.service.comment.domain.ReplyDto
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/boards/{boardId}/comments") // 게시글 ID 아래에 댓글 엔드포인트
class CommentController(private val commentService: CommentService) {

    // 특정 게시글의 모든 댓글 조회
    @GetMapping
    fun getComments(@PathVariable boardId: Long): List<ReplyDto> {
        return commentService.getCommentsByBoardId(boardId)
    }

    // 댓글 생성 (일반 댓글 또는 대댓글)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // 성공적으로 생성되었음을 나타내는 201 상태 코드 반환
    fun createComment(@PathVariable boardId: Long, @RequestBody request: CreateCommentRequest): ReplyDto {
        return commentService.createComment(boardId, request)
    }

    // 댓글 수정
    @PutMapping("/{commentId}")
    fun updateComment(
        @PathVariable boardId: Long,
        @PathVariable commentId: Long,
        @RequestBody request: UpdateCommentRequest
    ): CommentResponse {
        return commentService.updateComment(boardId, commentId, request)
    }

    // 댓글 삭제
    @DeleteMapping("/{replyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204 No Content
    fun deleteComment(
        @PathVariable boardId: Long,
        @PathVariable replyId: Int) {
        commentService.deleteComment(boardId, replyId)
    }
}
