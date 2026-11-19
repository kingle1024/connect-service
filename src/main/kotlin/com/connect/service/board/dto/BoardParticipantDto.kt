package com.connect.service.board.dto

import com.connect.service.board.entity.BoardParticipant
import java.time.LocalDateTime

data class BoardParticipantDto(
    val userId: String,
    val userName: String,
    val joinedDts: LocalDateTime,
) {
    companion object {
        fun from(p: BoardParticipant) = BoardParticipantDto(
            userId = p.userId,
            userName = p.userName,
            joinedDts = p.insertDts,
        )
    }
}
