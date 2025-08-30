package com.connect.service.comment

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/boards/{boardId}/comments") // 게시글 ID 아래에 댓글 엔드포인트
class CommentController(private val commentService: CommentService) {

    // 특정 게시글의 모든 댓글 조회
    @GetMapping
    fun getComments(@PathVariable boardId: Long): List<CommentResponse> {
        return commentService.getCommentsByBoardId(boardId)
    }

    // 댓글 생성 (일반 댓글 또는 대댓글)
    @PostMapping
    fun createComment(
        @PathVariable boardId: Long,
        @RequestBody request: CreateCommentRequest
    ): ResponseEntity<CommentResponse> {
        val newComment = commentService.createComment(boardId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(newComment)
    }

    // 댓글 수정
    @PutMapping("/{commentId}")
    fun updateComment(
        @PathVariable commentId: Long,
        @RequestBody request: UpdateCommentRequest
    ): CommentResponse {
        return commentService.updateComment(commentId, request)
    }

    // 댓글 삭제
    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204 No Content
    fun deleteComment(@PathVariable commentId: Long) {
        commentService.deleteComment(commentId)
    }
}
