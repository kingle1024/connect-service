package com.connect.service.comment.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Entity // 이 클래스가 JPA 엔티티임을 명시
@Table(name = "reply") // 매핑될 데이터베이스 테이블 이름을 "reply"로 지정
data class ReplyEntity(
    @Id // 기본 키(Primary Key)임을 명시
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB의 AUTO_INCREMENT 기능을 사용 (ID 자동 생성)
    val id: Int = 0,             // 댓글/대댓글 고유 ID (Reply.id에 해당, DB가 자동 생성)

    @Column(name = "post_id", nullable = false) // 'post_id' 컬럼과 매핑, NOT NULL
    val postId: Long,        // 해당 댓글이 속한 게시글의 ID

    @Column(name = "user_id", nullable = false) // 'user_id' 컬럼과 매핑, NOT NULL
    val userId: String,      // 댓글 작성자 ID

    @Column(name = "user_name", nullable = false) // 'user_name' 컬럼과 매핑, NOT NULL
    val userName: String,    // 댓글 작성자 이름

    @Column(name = "title") // 'title' 컬럼과 매핑, NULL 허용
    val title: String?,      // 최상위 댓글 제목 (대댓글은 NULL)

    @Column(name = "content", nullable = false, columnDefinition = "TEXT") // 'content' 컬럼과 매핑, NOT NULL, TEXT 타입
    var content: String,     // 댓글 내용

    @Column(name = "insert_dts", nullable = false) // 'insert_dts' 컬럼과 매핑, NOT NULL
    val insertDts: LocalDateTime, // 댓글 작성일시 (LocalDateTime 객체)

    @Column(name = "update_dts")
    var updateDts: LocalDateTime? = null,

    @Column(name = "parent_reply_id") // 'parent_reply_id' 컬럼과 매핑, NULL 허용
    val parentReplyId: Int?  // 대댓글인 경우 부모 댓글의 ID (최상위 댓글은 NULL)
)

fun ReplyEntity.toReplyDto(boardId: Long): ReplyDto { // 'fun' 키워드 앞에 아무런 접근 제한자(private 등) 없음 -> public 최상위 함수
    return ReplyDto(
        id = this.id,
        userId = this.userId,
        userName = this.userName,
        title = this.title,
        content = this.content,
        insertDts = this.insertDts.format(DateTimeFormatter.ISO_DATE_TIME),
        parentId = this.parentReplyId ?: boardId.toInt(), // Long을 Int로 캐스팅
        replies = null
    )
}
