package com.connect.service.admin.dto

import com.connect.service.board.entity.BoardMst
import java.time.LocalDateTime

data class AdminBoardDto(
    val id: Long,
    val title: String,
    val content: String,
    val category: String,
    val userId: String,
    val userName: String,
    val commentCount: Long,
    val destination: String,
    val maxCapacity: Int,
    val currentParticipants: Int,
    val deadlineDts: LocalDateTime,
    val insertDts: LocalDateTime?,
    val updateDts: LocalDateTime?,
    val deleted: Boolean
) {
    companion object {
        fun from(board: BoardMst): AdminBoardDto = AdminBoardDto(
            id = board.id ?: throw IllegalStateException("게시글 ID가 없습니다."),
            title = board.title,
            content = board.content,
            category = board.category,
            userId = board.userId,
            userName = board.userName,
            commentCount = board.commentCount,
            destination = board.destination,
            maxCapacity = board.maxCapacity,
            currentParticipants = board.currentParticipants,
            deadlineDts = board.deadlineDts,
            insertDts = board.insertDts,
            updateDts = board.updateDts,
            deleted = board.isDeleted
        )
    }
}
