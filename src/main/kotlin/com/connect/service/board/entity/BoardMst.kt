package com.connect.service.board.entity

import com.connect.service.comment.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "board_mst")
data class BoardMst(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    var title: String,
    var content: String,
    var category: String,
    var commentCount: Long = 0,
    var userId: String,
    var userName: String,
    var deadlineDts: LocalDateTime,
    var destination: String,
    var maxCapacity: Int,
    var currentParticipants: Int,

    var isDeleted: Boolean = false,
) : BaseEntity()
