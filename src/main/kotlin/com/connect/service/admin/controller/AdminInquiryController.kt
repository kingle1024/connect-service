package com.connect.service.admin.controller

import com.connect.service.admin.dto.PageResponse
import com.connect.service.admin.service.AdminInquiryService
import com.connect.service.common.ApiResponse
import com.connect.service.inquiry.dto.InquiryAnswerRequest
import com.connect.service.inquiry.dto.InquiryDto
import com.connect.service.inquiry.dto.InquiryStatusRequest
import com.connect.service.inquiry.entity.InquiryStatus
import com.connect.service.inquiry.entity.InquiryType
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
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
@RequestMapping("/api/admin/inquiries")
class AdminInquiryController(
    private val adminInquiryService: AdminInquiryService
) {

    /**
     * @param deleted null 이면 숨김 여부와 무관하게 전체, true 면 숨긴 문의만, false 면 정상 문의만
     */
    @GetMapping
    fun list(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) type: InquiryType?,
        @RequestParam(required = false) status: InquiryStatus?,
        @RequestParam(required = false) deleted: Boolean?,
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<ApiResponse<PageResponse<InquiryDto>>> =
        ResponseEntity.ok(
            ApiResponse.success(data = adminInquiryService.search(keyword, type, status, deleted, pageable))
        )

    // 사이드바 배지용 미답변 건수
    @GetMapping("/pending-count")
    fun pendingCount(): ResponseEntity<ApiResponse<Long>> =
        ResponseEntity.ok(ApiResponse.success(data = adminInquiryService.countPending()))

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): ResponseEntity<ApiResponse<InquiryDto>> =
        ResponseEntity.ok(ApiResponse.success(data = adminInquiryService.get(id)))

    @PutMapping("/{id}/answer")
    fun answer(
        @PathVariable id: Long,
        @RequestBody request: InquiryAnswerRequest,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<InquiryDto>> =
        ResponseEntity.ok(
            ApiResponse.success("답변을 등록했습니다.", adminInquiryService.answer(id, authentication.name, request))
        )

    @PutMapping("/{id}/status")
    fun changeStatus(
        @PathVariable id: Long,
        @RequestBody request: InquiryStatusRequest
    ): ResponseEntity<ApiResponse<InquiryDto>> =
        ResponseEntity.ok(
            ApiResponse.success("상태를 변경했습니다.", adminInquiryService.changeStatus(id, request.status))
        )

    // 소프트 삭제(숨김). 아래 restore 로 되돌릴 수 있다.
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<ApiResponse<InquiryDto>> =
        ResponseEntity.ok(ApiResponse.success("문의를 숨겼습니다.", adminInquiryService.setDeleted(id, true)))

    @PostMapping("/{id}/restore")
    fun restore(@PathVariable id: Long): ResponseEntity<ApiResponse<InquiryDto>> =
        ResponseEntity.ok(ApiResponse.success("문의를 복원했습니다.", adminInquiryService.setDeleted(id, false)))
}
