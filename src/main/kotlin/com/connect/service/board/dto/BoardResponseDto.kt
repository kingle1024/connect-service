package com.connect.service.board.dto

import com.connect.service.board.entity.BoardMst
import java.time.LocalDateTime

data class BoardResponseDto(
    val id: Long, // 게시글 ID (Long을 JavaScript number로 변환)
    val title: String, // 제목
    val content: String, // 내용
    val category: String, // 카테고리
    val commentCount: Long, // 댓글 갯수
    val userId: String, // 작성자 ID
    val userName: String, // 작성자 이름
    val insertDts: LocalDateTime, // 등록일 (LocalDateTime -> String 변환은 나중에 수행)
    val deadlineDts: LocalDateTime, // 마감일 (LocalDateTime -> String 변환은 나중에 수행)
    val destination: String, // 목적지
    val maxCapacity: Int, // 최대 모집 인원
    val currentParticipants: Int // 모집 인원
) {
    // BoardMst 엔티티를 BoardResponseDto로 변환하는 팩토리 메서드
    companion object {
        fun from(boardMst: BoardMst): BoardResponseDto {
            return BoardResponseDto(
                id = boardMst.id ?: throw IllegalArgumentException("Board ID cannot be null"), // id는 null이 아님을 가정
                title = boardMst.title,
                content = boardMst.content,
                category = boardMst.category,
                commentCount = boardMst.commentCount,
                userId = boardMst.userId,
                userName = boardMst.userName,
                insertDts = boardMst.insertDts ?: throw IllegalArgumentException("Insert date cannot be null"), // BaseEntity로부터 가져옴
                deadlineDts = boardMst.deadlineDts,
                destination = boardMst.destination,
                maxCapacity = boardMst.maxCapacity,
                currentParticipants = boardMst.currentParticipants
            )
        }
    }
}
