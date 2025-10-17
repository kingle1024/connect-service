package com.connect.service.comment.domain

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class CommentEntity(
    val id: Int,             // 댓글/대댓글 고유 ID (Primary Key)
    val postId: Long,        // 해당 댓글이 속한 게시글 ID (최상위 댓글과 대댓글 모두 이 postId를 가짐)
    val userId: String,      // 댓글 작성자 ID
    val userName: String,    // 이름
    val title: String?,      // 타이틀 (nullable: 대댓글인 경우 null)
    val content: String,     // 내용
    val insertDts: LocalDateTime, // 작성일 (LocalDateTime 객체)
    val parentReplyId: Int?  // 대댓글인 경우 부모 댓글의 ID, 최상위 댓글인 경우 null
)
