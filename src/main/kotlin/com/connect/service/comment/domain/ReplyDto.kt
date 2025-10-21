package com.connect.service.comment.domain

data class ReplyDto( // 클래스 이름 변경!
    val id: Int,             // 댓글/대댓글 고유 ID
    val userId: String,      // 댓글 작성자 ID
    val userName: String,    // 이름
    val title: String?,      // 타이틀 (nullable: 대댓글인 경우 제목이 없음)
    val content: String,     // 내용
    val insertDts: String,   // 작성일 (ISO 8601 형식 문자열)
    val parentId: Int?,      // 대댓글인 경우 부모 댓글 ID, 최초 댓글인 경우 게시글 ID
    val replies: List<ReplyDto>? // 대댓글 배열 (선택적) -> 여기에 사용된 타입도 ReplyDto로 변경!
)
