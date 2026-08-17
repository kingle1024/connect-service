package com.connect.service.admin.controller

import com.connect.service.category.dto.CategoryDto
import com.connect.service.category.dto.CategoryOrderRequest
import com.connect.service.category.dto.CategorySaveRequest
import com.connect.service.category.service.CategoryService
import com.connect.service.common.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/categories")
class AdminCategoryController(
    private val categoryService: CategoryService
) {

    @GetMapping
    fun list(): ResponseEntity<ApiResponse<List<CategoryDto>>> =
        ResponseEntity.ok(ApiResponse.success(data = categoryService.getAllForAdmin()))

    @PostMapping
    fun create(@RequestBody request: CategorySaveRequest): ResponseEntity<ApiResponse<CategoryDto>> =
        ResponseEntity.ok(ApiResponse.success("카테고리를 추가했습니다.", categoryService.create(request)))

    // 드래그로 바뀐 순서 저장. "/{id}" 보다 구체적인 경로이므로 먼저 매칭된다.
    @PutMapping("/order")
    fun reorder(@RequestBody request: CategoryOrderRequest): ResponseEntity<ApiResponse<List<CategoryDto>>> {
        categoryService.reorder(request.ids)
        return ResponseEntity.ok(ApiResponse.success("순서를 변경했습니다.", categoryService.getAllForAdmin()))
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: CategorySaveRequest
    ): ResponseEntity<ApiResponse<CategoryDto>> =
        ResponseEntity.ok(ApiResponse.success("카테고리를 수정했습니다.", categoryService.update(id, request)))

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<ApiResponse<Unit>> {
        categoryService.delete(id)
        return ResponseEntity.ok(ApiResponse.success("카테고리를 삭제했습니다."))
    }
}
