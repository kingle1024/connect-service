package com.connect.service.inquiry.controller

import com.connect.service.common.ApiResponse
import com.connect.service.inquiry.dto.InquiryCreateRequest
import com.connect.service.inquiry.dto.InquiryDto
import com.connect.service.inquiry.service.InquiryService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 앱의 "개선/버그 요청" 화면용 API.
 *
 * SecurityConfig 에서 이 경로를 permitAll 로 열어두고(웹 CORS preflight 통과용)
 * 실제 인증은 JwtAuthenticationFilter 가 채워준 Authentication 으로 여기서 확인한다.
 * (계정 관련 API 와 같은 방식)
 *
 * 다만 문의는 로그인하지 않아도 접수할 수 있다.
 * 비로그인 접수는 단말이 만든 guestKey 로 작성자를 구분하고, 조회할 때도 같은 키를 넘겨야 한다.
 */
@RestController
@RequestMapping("/api/inquiries")
class InquiryController(
    private val inquiryService: InquiryService
) {

    /** 문의 접수. 로그인하지 않았다면 본문의 guestKey 로 작성자를 구분한다. */
    @PostMapping
    fun create(
        @RequestBody request: InquiryCreateRequest,
        authentication: Authentication?
    ): ResponseEntity<ApiResponse<InquiryDto>> =
        ResponseEntity.ok(
            ApiResponse.success("요청이 접수되었습니다.", inquiryService.create(authentication?.name, request))
        )

    /**
     * 내가 접수한 문의 목록 (최신순).
     *
     * 로그인 상태면 계정 기준이라 guestKey 는 무시한다.
     * 비로그인이면 guestKey 기준이고, 키가 없으면 빈 목록을 돌려준다.
     */
    @GetMapping("/my")
    fun myList(
        @RequestParam(required = false) guestKey: String?,
        authentication: Authentication?
    ): ResponseEntity<ApiResponse<List<InquiryDto>>> =
        ResponseEntity.ok(
            ApiResponse.success(data = inquiryService.getMyInquiries(authentication?.name, guestKey))
        )

    /** 내 문의 상세 (답변 포함) */
    @GetMapping("/my/{id}")
    fun myDetail(
        @PathVariable id: Long,
        @RequestParam(required = false) guestKey: String?,
        authentication: Authentication?
    ): ResponseEntity<ApiResponse<InquiryDto>> =
        ResponseEntity.ok(
            ApiResponse.success(data = inquiryService.getMyInquiry(authentication?.name, guestKey, id))
        )
}
