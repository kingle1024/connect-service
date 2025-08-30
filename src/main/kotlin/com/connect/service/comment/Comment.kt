package com.connect.service.comment

import jakarta.persistence.* // JPA 어노테이션 임포트
import java.time.LocalDateTime

@Entity
@Table(name = "connect_comment_mst") // 데이터베이스 테이블 이름
data class CommentMst(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) // ID 자동 생성 전략
    val id: Long? = null,

    @Column(nullable = false)
    var content: String,

    @Column(nullable = false)
    val author: String,

    @Column(nullable = false)
    val boardId: Long,

    val parentCommentId: Long? = null, // 대댓글인 경우 부모 댓글의 ID (최상위 댓글은 null)

    var isDeleted: Boolean = false, // 댓글 삭제 여부

    val insertDts: LocalDateTime = LocalDateTime.now(), // 댓글 생성 시간
    var updateDts: LocalDateTime = LocalDateTime.now() // 댓글 수정 시간
)
