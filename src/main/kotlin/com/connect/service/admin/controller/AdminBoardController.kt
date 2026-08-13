package com.connect.service.admin.controller

import com.connect.service.admin.dto.AdminBoardDto
import com.connect.service.admin.dto.AdminBoardUpdateRequest
import com.connect.service.admin.dto.PageResponse
import com.connect.service.admin.service.AdminBoardService
import com.connect.service.common.ApiResponse
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/boards")
class AdminBoardController(
    private val adminBoardService: AdminBoardService
) {

    /**
     * @param deleted null 이면 삭제 여부와 무관하게 전체, true 면 삭제된 글만, false 면 정상 글만
     */
    @GetMapping
    fun list(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) deleted: Boolean?,
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<ApiResponse<PageResponse<AdminBoardDto>>> =
        ResponseEntity.ok(
            ApiResponse.success(data = adminBoardService.search(keyword, category, deleted, pageable))
        )

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): ResponseEntity<ApiResponse<AdminBoardDto>> =
        ResponseEntity.ok(ApiResponse.success(data = adminBoardService.get(id)))

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: AdminBoardUpdateRequest
    ): ResponseEntity<ApiResponse<AdminBoardDto>> =
        ResponseEntity.ok(ApiResponse.success("게시물을 수정했습니다.", adminBoardService.update(id, request)))

    // 소프트 삭제 (숨김). 아래 restore 로 되돌릴 수 있다.
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<ApiResponse<AdminBoardDto>> =
        ResponseEntity.ok(ApiResponse.success("게시물을 삭제했습니다.", adminBoardService.setDeleted(id, true)))

    @PostMapping("/{id}/restore")
    fun restore(@PathVariable id: Long): ResponseEntity<ApiResponse<AdminBoardDto>> =
        ResponseEntity.ok(ApiResponse.success("게시물을 복원했습니다.", adminBoardService.setDeleted(id, false)))
}
