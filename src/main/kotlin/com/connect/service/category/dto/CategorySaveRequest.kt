package com.connect.service.category.dto

data class CategorySaveRequest(
    val name: String,
    // 순서는 백오피스에서 드래그로 정하므로 보통 비워서 보낸다.
    // 신규 등록 시 null 이면 맨 뒤에 붙고, 수정 시 null 이면 기존 순서를 유지한다.
    val sortOrder: Int? = null,
    val active: Boolean = true
)

// 드래그로 바뀐 전체 순서. 배열 순서대로 sortOrder 를 0,1,2... 로 다시 매긴다.
data class CategoryOrderRequest(
    val ids: List<Long>
)
