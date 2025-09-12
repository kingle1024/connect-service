package com.connect.service.board.dto

data class UpdateBoardRequest(
    val id: Long, // 🌟 수정할 게시글의 ID 🌟 (BoardMst의 ID 타입과 동일하게)
    val title: String,
    val content: String,
    val author: String? = null // 작성자는 변경되지 않거나 선택 사항일 수 있으므로 nullable로 설정 (필요에 따라)
)
