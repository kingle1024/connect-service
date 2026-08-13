package com.connect.service.admin.controller

import com.connect.service.admin.dto.AdminUserDto
import com.connect.service.admin.dto.AdminUserEnabledRequest
import com.connect.service.admin.dto.AdminUserPasswordRequest
import com.connect.service.admin.dto.AdminUserRolesRequest
import com.connect.service.admin.dto.PageResponse
import com.connect.service.admin.service.AdminUserService
import com.connect.service.common.ApiResponse
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/users")
class AdminUserController(
    private val adminUserService: AdminUserService
) {

    /**
     * @param role ROLE_USER / ROLE_ADMIN / ROLE_VERIFIED 중 하나. 비어 있으면 전체
     * @param enabled null 이면 정지 여부와 무관하게 전체
     */
    @GetMapping
    fun list(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) role: String?,
        @RequestParam(required = false) enabled: Boolean?,
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<ApiResponse<PageResponse<AdminUserDto>>> =
        ResponseEntity.ok(
            ApiResponse.success(data = adminUserService.search(keyword, role, enabled, pageable))
        )

    @GetMapping("/{userId}")
    fun get(@PathVariable userId: String): ResponseEntity<ApiResponse<AdminUserDto>> =
        ResponseEntity.ok(ApiResponse.success(data = adminUserService.get(userId)))

    @PutMapping("/{userId}/roles")
    fun updateRoles(
        @PathVariable userId: String,
        @RequestBody request: AdminUserRolesRequest,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<AdminUserDto>> =
        ResponseEntity.ok(
            ApiResponse.success(
                "권한을 변경했습니다.",
                adminUserService.updateRoles(userId, request.roles, authentication.name)
            )
        )

    @PutMapping("/{userId}/enabled")
    fun updateEnabled(
        @PathVariable userId: String,
        @RequestBody request: AdminUserEnabledRequest,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<AdminUserDto>> {
        val message = if (request.enabled) "계정을 활성화했습니다." else "계정을 정지했습니다."
        return ResponseEntity.ok(
            ApiResponse.success(
                message,
                adminUserService.setEnabled(userId, request.enabled, authentication.name)
            )
        )
    }

    @PutMapping("/{userId}/password")
    fun resetPassword(
        @PathVariable userId: String,
        @RequestBody request: AdminUserPasswordRequest
    ): ResponseEntity<ApiResponse<AdminUserDto>> =
        ResponseEntity.ok(
            ApiResponse.success(
                "비밀번호를 변경했습니다. 해당 회원의 기존 로그인 세션은 모두 해제됩니다.",
                adminUserService.resetPassword(userId, request.newPassword)
            )
        )
}
