package com.connect.service.admin.dto

import org.springframework.data.domain.Page

// 백오피스 목록 화면용 공통 페이지 응답
data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
) {
    companion object {
        fun <E, T> from(page: Page<E>, mapper: (E) -> T): PageResponse<T> = PageResponse(
            content = page.content.map(mapper),
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages
        )
    }
}
