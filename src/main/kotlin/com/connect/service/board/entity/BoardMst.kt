package com.connect.service.board.entity

import com.connect.service.comment.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "board_mst")
data class BoardMst(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    var title: String,
    var content: String,
    var author: String,
    var targetPlace: String,
    var viewCount: Long = 0,
    var isDeleted: Boolean = false,
) : BaseEntity()
