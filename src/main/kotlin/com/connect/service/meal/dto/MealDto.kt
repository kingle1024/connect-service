package com.connect.service.meal.dto

// 식단 한 품목 — 코너(한식/라면/간편식 등) + 메인메뉴 + 반찬 + 칼로리 + 사진
data class MealItemDto(
    val corner: String,
    val name: String,
    val side: String,
    val kcal: Int,
    val thumbnailUrl: String
)

// 하루치 식단 (조식/중식/석식)
data class MealsDto(
    val morning: List<MealItemDto>,
    val lunch: List<MealItemDto>,
    val dinner: List<MealItemDto>
) {
    // 사람들이 주로 보는 ★중식★이 1개 이상 있고 전부 사진까지 붙었을 때 "완료"로 본다.
    // 아침엔 프레시밀이 식단·사진을 아직 안 올렸거나 일부만 올린 경우가 있어 그때는 미완료로 취급한다.
    fun isComplete(): Boolean = lunch.isNotEmpty() && lunch.all { it.thumbnailUrl.isNotBlank() }

    // 재조회 결과가 기존 캐시보다 "더 채워졌는지" 비교용 점수 — 중식 품목 수 우선, 사진 수로 보조.
    fun score(): Int = lunch.size * 100 + lunch.count { it.thumbnailUrl.isNotBlank() }

    companion object {
        val EMPTY = MealsDto(emptyList(), emptyList(), emptyList())
    }
}

// 오늘 식단 응답 — cached/partial 은 UI 안내용(캐시 응답인지, 아직 다 안 올라온 상태인지)
data class TodayMealResponse(
    val campus: String,
    val date: String,
    val meals: MealsDto,
    val cached: Boolean = false,
    val partial: Boolean = false
)
