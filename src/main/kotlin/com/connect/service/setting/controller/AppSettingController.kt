package com.connect.service.setting.controller

import com.connect.service.common.ApiResponse
import com.connect.service.setting.dto.ThemeSettingDto
import com.connect.service.setting.service.AppSettingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// 앱이 시작할 때 읽는 공개 설정 (api/public 아래는 로그인 불필요)
@RestController
@RequestMapping("/api/public/settings")
class PublicSettingController(
    private val appSettingService: AppSettingService
) {
    @GetMapping("/theme")
    fun theme(): ResponseEntity<ApiResponse<ThemeSettingDto>> =
        ResponseEntity.ok(ApiResponse.success(data = appSettingService.getTheme()))
}

// 백오피스 설정 화면 (api/admin 아래는 ROLE_ADMIN 만)
@RestController
@RequestMapping("/api/admin/settings")
class AdminSettingController(
    private val appSettingService: AppSettingService
) {
    @GetMapping("/theme")
    fun theme(): ResponseEntity<ApiResponse<ThemeSettingDto>> =
        ResponseEntity.ok(ApiResponse.success(data = appSettingService.getTheme()))

    @PutMapping("/theme")
    fun updateTheme(@RequestBody request: ThemeSettingDto): ResponseEntity<ApiResponse<ThemeSettingDto>> =
        try {
            ResponseEntity.ok(ApiResponse.success("제품 색을 저장했습니다.", appSettingService.updateTheme(request)))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "잘못된 값입니다."))
        }
}
