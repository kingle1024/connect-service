package com.connect.service.category.repository

import com.connect.service.category.entity.CategoryMst
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CategoryRepository : JpaRepository<CategoryMst, Long> {
    // 백오피스 목록 (비활성 포함)
    fun findAllByOrderBySortOrderAscNameAsc(): List<CategoryMst>

    // 앱에 노출할 목록 (활성만)
    fun findAllByIsActiveTrueOrderBySortOrderAscNameAsc(): List<CategoryMst>

    fun findByName(name: String): CategoryMst?
}
