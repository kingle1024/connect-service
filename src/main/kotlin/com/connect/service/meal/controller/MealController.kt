package com.connect.service.meal.controller

import com.connect.service.common.ApiResponse
import com.connect.service.meal.dto.TodayMealResponse
import com.connect.service.meal.service.MealService
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/public/meal") // /api/public/** 는 SecurityConfig 에서 허용 (사내 식단이라 로그인 불필요)
@CrossOrigin
class MealController(
    private val mealService: MealService
) {
    // 오늘 식단 조회 — campus: gangchon(기본) | eulji, force=true 면 캐시 무시하고 재조회
    @GetMapping
    fun todayMeal(
        @RequestParam(required = false) campus: String?,
        @RequestParam(required = false, defaultValue = "false") force: Boolean
    ): ResponseEntity<ApiResponse<TodayMealResponse>> {
        return try {
            ResponseEntity.ok(ApiResponse.success(data = mealService.getTodayMeal(campus, force)))
        } catch (e: Exception) {
            logger.warn { "오늘 식단 조회 실패(campus=$campus): ${e.message}" }
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("식단을 불러오지 못했습니다. 잠시 후 다시 시도해주세요."))
        }
    }
}
