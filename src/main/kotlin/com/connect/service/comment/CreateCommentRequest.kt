package com.connect.service.comment

import java.time.LocalDateTime

data class CreateCommentRequest(
    val content: String,
    val userId: String, // 댓글 작성자 ID
    val userName: String, // 댓글 작성자 이름
    val parentId: Long? = null // 대댓글인 경우 부모 CommentMst의 ID, 일반 댓글은 null
)

data class CommentResponse(
    val id: Long,
    val userId: String,
    val userName: String,
    val content: String,
    val insertDts: LocalDateTime,
    val updateDts: LocalDateTime,
    val isDeleted: Boolean,
    val parentId: Long?, // 대댓글인 경우 부모 댓글의 ID (CommentMst.id), 일반 댓글은 null
    val replies: MutableList<CommentResponse> = mutableListOf() // 이 댓글의 대댓글 목록
) {
    companion object {
        // CommentMst 엔티티로부터 CommentResponse 생성
        fun from(commentMst: CommentMst): CommentResponse {
            return CommentResponse(
                id = commentMst.id!!,
                userId = commentMst.userId,
                userName = commentMst.userName,
                content = commentMst.content,
                insertDts = commentMst.insertDts,
                updateDts = commentMst.updateDts,
                isDeleted = commentMst.isDeleted,
                parentId = null // CommentMst는 부모가 없어
            )
        }

        // CommentDtl 엔티티로부터 CommentResponse 생성
        fun from(commentDtl: CommentDtl): CommentResponse {
            return CommentResponse(
                id = commentDtl.id!!,
                userId = commentDtl.userId,
                userName = commentDtl.userName,
                content = commentDtl.content,
                insertDts = commentDtl.insertDts,
                updateDts = commentDtl.updateDts,
                isDeleted = commentDtl.isDeleted,
                parentId = commentDtl.parentId // CommentDtl은 부모 ID가 있어
            )
        }
    }
}

// 댓글 수정 요청을 위한 DTO
data class UpdateCommentRequest(
    val content: String // 수정 가능한 필드만 포함
)
