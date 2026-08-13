package com.connect.service.meal.service

import com.connect.service.meal.client.CjFreshMealClient
import com.connect.service.meal.dto.MealsDto
import com.connect.service.meal.dto.TodayMealResponse
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * 오늘 구내식당 식단 조회 — 캠퍼스(강촌/을지)별 당일 캐시.
 *
 * 식단은 하루 단위로만 바뀌므로 같은 날 재호출은 캐시로 응답한다(force=true 면 갱신).
 * 다만 아침엔 프레시밀이 식단·사진을 아직 다 안 올린 "미완료" 상태일 수 있어, 그 캐시는
 * 굳히지 않고 [PARTIAL_REFRESH_WINDOW_MS] 가 지나면 다시 받아 더 채워진 쪽으로 교체한다.
 */
@Service
class MealService(
    private val cjFreshMealClient: CjFreshMealClient
) {
    private val cache = ConcurrentHashMap<String, CacheEntry>()

    fun getTodayMeal(campus: String?, force: Boolean = false): TodayMealResponse {
        val target = MealCampus.of(campus)
        val today = today()
        val cached = cache[target.code]?.takeIf { it.date == today }

        if (!force && cached != null && !cached.needsRefresh()) {
            return TodayMealResponse(
                campus = target.code,
                date = today,
                meals = cached.meals,
                cached = true,
                partial = !cached.meals.isComplete()
            )
        }

        return try {
            val fetched = cjFreshMealClient.fetchToday(target.storeIdx)
            // 재조회가 기존 캐시보다 덜 채워졌으면(프레시밀이 일시적으로 빈 응답) 기존 캐시를 유지한다.
            val meals = if (cached != null && cached.meals.score() > fetched.score()) cached.meals else fetched
            cache[target.code] = CacheEntry(today, meals)
            TodayMealResponse(
                campus = target.code,
                date = today,
                meals = meals,
                partial = !meals.isComplete()
            )
        } catch (e: Exception) {
            // 조회 실패해도 같은 날 캐시가 있으면 그걸로 응답한다.
            if (cached != null) {
                logger.warn { "식단 갱신 실패 — 당일 캐시로 응답합니다(campus=${target.code}): ${e.message}" }
                return TodayMealResponse(
                    campus = target.code,
                    date = today,
                    meals = cached.meals,
                    cached = true,
                    partial = !cached.meals.isComplete()
                )
            }
            throw e
        }
    }

    private fun today(): String = LocalDate.now(KST).format(DATE_FORMAT)

    private class CacheEntry(
        val date: String,
        val meals: MealsDto,
        val fetchedAt: Long = System.currentTimeMillis()
    ) {
        // 미완료 캐시는 잠깐만 쓰고 다시 받는다. 완료된 캐시는 그날 하루 그대로 쓴다.
        fun needsRefresh(): Boolean =
            !meals.isComplete() && System.currentTimeMillis() - fetchedAt > PARTIAL_REFRESH_WINDOW_MS
    }

    enum class MealCampus(val code: String, val storeIdx: String) {
        GANGCHON("gangchon", "6486"),
        EULJI("eulji", "5924");

        companion object {
            fun of(code: String?): MealCampus =
                entries.firstOrNull { it.code.equals(code?.trim(), ignoreCase = true) } ?: GANGCHON
        }
    }

    companion object {
        private val KST: ZoneId = ZoneId.of("Asia/Seoul")
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        private const val PARTIAL_REFRESH_WINDOW_MS = 10 * 60 * 1000L // 10분
    }
}
