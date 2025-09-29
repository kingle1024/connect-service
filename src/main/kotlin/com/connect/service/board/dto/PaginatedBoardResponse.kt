package com.connect.service.board.dto

data class PaginatedBoardResponse(
    val nextPageToken: Int?, // 다음 페이지 번호 (없으면 null 또는 0)
    val posts: List<BoardResponseDto> // 게시글 목록
)
