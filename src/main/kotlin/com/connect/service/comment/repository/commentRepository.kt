package com.connect.service.comment.repository

import com.connect.service.comment.domain.CommentEntity

interface CommentRepository { // 일반적인 인터페이스 예시
    /**
     * 특정 게시글(postId)에 속하는 모든 댓글과 대댓글을 가져옵니다.
     * 게시글에 대한 모든 계층형 댓글 데이터를 한 번에 조회하는 것이 중요합니다.
     * 보통 insertDts 기준으로 정렬하여 가져오는 것이 좋습니다.
     */
    fun findAllByPostIdOrderByInsertDtsAsc(postId: Long): List<CommentEntity>
    fun findById(id: Int): CommentEntity?
    fun save(commentEntity: CommentEntity): CommentEntity
    fun findMaxId(): Int?
}
