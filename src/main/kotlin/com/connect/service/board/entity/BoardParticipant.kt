package com.connect.service.board.entity

import com.connect.service.comment.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 모집글 참여자. 작성자는 넣지 않는다 (board_mst.current_participants 가 작성자 1명을 포함해 시작).
 * 테이블: src/main/resources/db/board-participant.sql
 */
@Entity
@Table(name = "board_participant")
class BoardParticipant(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val boardId: Long,
    var userId: String,
    var userName: String,
) : BaseEntity()
