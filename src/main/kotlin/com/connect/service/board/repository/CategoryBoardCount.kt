package com.connect.service.board.repository

// 카테고리별 게시글 수 집계 결과 (백오피스 카테고리 목록에서 사용)
interface CategoryBoardCount {
    fun getCategory(): String?
    fun getBoardCount(): Long
}
