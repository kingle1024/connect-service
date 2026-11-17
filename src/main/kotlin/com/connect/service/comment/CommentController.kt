package com.connect.service.comment

import com.connect.service.comment.domain.ReplyDto
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

/**
 * 댓글 API. 조회는 누구나, 작성·수정·삭제는 인증된 사람만 한다.
 * 인증은 회원이면 로그인 토큰, 비회원이면 게스트 쿠키다. (GuestCookieAuthenticationFilter)
 * 작성자는 요청 본문이 아니라 인증 정보로 정하고, 수정·삭제는 작성자 본인만 할 수 있다.
 */
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
    fun createComment(
        @PathVariable boardId: Long,
        @RequestBody request: CreateCommentRequest,
        authentication: Authentication?
    ): ReplyDto {
        return commentService.createComment(boardId, request, requireUserId(authentication))
    }

    // 댓글 수정
    @PutMapping("/{replyId}")
    fun updateComment(
        @PathVariable boardId: Long,
        @PathVariable replyId: Int,
        @RequestBody request: UpdateCommentRequest,
        authentication: Authentication?
    ): ReplyDto {
        return commentService.updateComment(boardId, replyId, request, requireUserId(authentication))
    }

    // 댓글 삭제
    @DeleteMapping("/{replyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204 No Content
    fun deleteComment(
        @PathVariable boardId: Long,
        @PathVariable replyId: Int,
        authentication: Authentication?
    ) {
        commentService.deleteComment(boardId, replyId, requireUserId(authentication))
    }

    private fun requireUserId(authentication: Authentication?): String =
        authentication?.name
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "작성자를 확인할 수 없습니다. 다시 시도해주세요.")
}
