package com.connect.service.category.service

import com.connect.service.board.repository.BoardRepository
import com.connect.service.category.dto.CategoryDto
import com.connect.service.category.dto.CategorySaveRequest
import com.connect.service.category.entity.CategoryMst
import com.connect.service.category.repository.CategoryRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val boardRepository: BoardRepository
) {

    /**
     * 앱에 노출할 활성 카테고리 이름 목록.
     *
     * category_mst 가 아직 비어 있으면(마이그레이션 스크립트 미실행 등) 기존처럼
     * 게시글에서 뽑아 쓴다. 백오피스 도입 때문에 앱의 카테고리 필터가 비는 일은 없도록 한다.
     */
    @Transactional(readOnly = true)
    fun getActiveCategoryNames(): List<String> {
        val managed = categoryRepository.findAllByIsActiveTrueOrderBySortOrderAscNameAsc().map { it.name }
        if (managed.isNotEmpty()) {
            return managed
        }
        return boardRepository.findDistinctCategories()
            .mapNotNull { it.trim().takeIf { trimmed -> trimmed.isNotEmpty() } }
            .distinct()
    }

    /** '기타' 칩인지 여부. 이 칩은 전용 칩이 없는 카테고리(직접입력 등)까지 함께 조회한다. */
    fun isEtcCategory(name: String): Boolean = name.trim() == ETC_CATEGORY_NAME

    @Transactional(readOnly = true)
    fun getAllForAdmin(): List<CategoryDto> {
        val counts = boardRepository.countGroupByCategory()
            .mapNotNull { row -> row.getCategory()?.let { it to row.getBoardCount() } }
            .toMap()

        return categoryRepository.findAllByOrderBySortOrderAscNameAsc()
            .map { CategoryDto.from(it, counts[it.name] ?: 0L) }
    }

    @Transactional
    fun create(request: CategorySaveRequest): CategoryDto {
        val name = normalizeName(request.name)
        if (categoryRepository.findByName(name) != null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 등록된 카테고리입니다: $name")
        }

        val saved = categoryRepository.save(
            CategoryMst(
                name = name,
                // 순서를 지정하지 않으면 목록 맨 뒤에 붙인다.
                sortOrder = request.sortOrder ?: nextSortOrder(),
                isActive = request.active
            )
        )
        return CategoryDto.from(saved, boardCountOf(name))
    }

    /**
     * 드래그로 바뀐 순서를 저장한다. 전달받은 배열 순서대로 sortOrder 를 0,1,2... 로 다시 매긴다.
     */
    @Transactional
    fun reorder(ids: List<Long>) {
        if (ids.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "정렬할 카테고리가 없습니다.")
        }
        if (ids.distinct().size != ids.size) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "중복된 카테고리가 있습니다.")
        }

        val categories = categoryRepository.findAllById(ids)
        if (categories.size != ids.size) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 카테고리가 포함되어 있습니다.")
        }

        val byId = categories.associateBy { it.id }
        ids.forEachIndexed { index, id -> byId[id]?.sortOrder = index }
        categoryRepository.saveAll(categories)
    }

    @Transactional
    fun update(id: Long, request: CategorySaveRequest): CategoryDto {
        val category = categoryRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다: $id") }

        val newName = normalizeName(request.name)
        val oldName = category.name

        if (newName != oldName) {
            categoryRepository.findByName(newName)?.let {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 등록된 카테고리입니다: $newName")
            }
            // board_mst 는 카테고리 이름을 문자열로 들고 있으므로 기존 게시글도 함께 갱신한다.
            boardRepository.updateCategoryName(oldName, newName)
        }

        category.name = newName
        // 순서는 드래그로 따로 관리하므로 값이 없으면 기존 순서를 유지한다.
        category.sortOrder = request.sortOrder ?: category.sortOrder
        category.isActive = request.active

        val saved = categoryRepository.save(category)
        return CategoryDto.from(saved, boardCountOf(newName))
    }

    @Transactional
    fun delete(id: Long) {
        val category = categoryRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다: $id") }

        // 게시글이 남아 있으면 삭제 시 그 게시글들이 관리되지 않는 카테고리로 떠버린다.
        // 노출만 막고 싶은 경우가 대부분이므로 비활성화를 안내한다.
        val boardCount = boardCountOf(category.name)
        if (boardCount > 0) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "이 카테고리를 사용 중인 게시물이 ${boardCount}건 있어 삭제할 수 없습니다. 비활성화를 사용해주세요."
            )
        }

        categoryRepository.delete(category)
    }

    private fun boardCountOf(name: String): Long = boardRepository.countByCategoryAndIsDeletedFalse(name)

    private fun nextSortOrder(): Int =
        (categoryRepository.findAllByOrderBySortOrderAscNameAsc().maxOfOrNull { it.sortOrder } ?: -1) + 1

    companion object {
        /**
         * 관리 목록에 없는(직접입력) 카테고리를 모아 보여주는 캐치올 카테고리 이름.
         * board_category_mst 에 이 이름이 없으면 캐치올 동작만 하지 않고 나머지는 그대로다.
         */
        const val ETC_CATEGORY_NAME = "기타"
    }

    private fun normalizeName(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "카테고리 이름을 입력해주세요.")
        }
        if (trimmed.length > 100) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "카테고리 이름은 100자 이하여야 합니다.")
        }
        return trimmed
    }
}
