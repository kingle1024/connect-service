package com.connect.service.comment

import java.time.LocalDateTime

data class CreateCommentRequest(
    val content: String,
    val author: String,
    val parentCommentId: Long? = null
)

data class CommentResponse(
    val id: Long,
    val content: String,
    val author: String,
    val boardId: Long,
    val parentCommentId: Long?,
    val isDeleted: Boolean,
    val insertDts: LocalDateTime,
    val updateDts: LocalDateTime,
    val replies: MutableList<CommentResponse> = mutableListOf() // 대댓글들을 담을 리스트 (계층형 조회 시 사용)
) {
    // Comment.kt 엔티티를 CommentResponse DTO로 변환하는 확장 함수 또는 동반 객체 함수
    companion object {
        fun from(comment: CommentMst): CommentResponse {
            return CommentResponse(
                id = comment.id!!,
                content = comment.content,
                author = comment.author,
                boardId = comment.boardId,
                parentCommentId = comment.parentCommentId,
                isDeleted = comment.isDeleted,
                insertDts = comment.insertDts,
                updateDts = comment.updateDts
            )
        }
    }
}

// 댓글 수정 요청을 위한 DTO
data class UpdateCommentRequest(
    val content: String // 수정 가능한 필드만 포함
)
