package com.connect.service.category.dto

import com.connect.service.category.entity.CategoryMst
import java.time.LocalDateTime

data class CategoryDto(
    val id: Long,
    val name: String,
    val sortOrder: Int,
    val active: Boolean,
    // 이 카테고리를 쓰는 삭제되지 않은 게시글 수 (삭제 가능 여부 판단용)
    val boardCount: Long,
    val insertDts: LocalDateTime?,
    val updateDts: LocalDateTime?
) {
    companion object {
        fun from(category: CategoryMst, boardCount: Long): CategoryDto = CategoryDto(
            id = category.id ?: throw IllegalStateException("카테고리 ID가 없습니다."),
            name = category.name,
            sortOrder = category.sortOrder,
            active = category.isActive,
            boardCount = boardCount,
            insertDts = category.insertDts,
            updateDts = category.updateDts
        )
    }
}
