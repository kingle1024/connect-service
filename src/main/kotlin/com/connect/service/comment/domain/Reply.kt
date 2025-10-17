package com.connect.service.comment.domain

data class Reply(
    val id: Int, // 댓글/대댓글 고유 ID
    val userId: String, // 댓글 작성자 ID
    val userName: String, // 이름
    val title: String?, // 타이틀 (nullable)
    val content: String, // 내용
    val insertDts: String, // 작성일
    val parentId: Int?, // 대댓글인 경우 부모 댓글 ID (nullable)
    val replies: List<Reply>? // 대댓글 배열 (선택적, nullable)
)
