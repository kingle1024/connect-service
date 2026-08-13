package com.connect.service.meal

import com.connect.service.meal.client.CjFreshMealClient
import com.connect.service.meal.dto.MealItemDto
import com.connect.service.meal.dto.MealsDto
import com.connect.service.meal.service.MealService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class MealServiceTest {

    @Mock
    private lateinit var cjFreshMealClient: CjFreshMealClient

    @InjectMocks
    private lateinit var mealService: MealService

    private fun item(name: String, thumbnail: String = "https://img/$name.jpg") =
        MealItemDto(corner = "한식", name = name, side = "김치", kcal = 700, thumbnailUrl = thumbnail)

    // 중식이 있고 사진까지 다 붙은 "완료" 식단
    private fun completeMeals() = MealsDto(
        morning = listOf(item("사골떡국")),
        lunch = listOf(item("제육볶음")),
        dinner = listOf(item("돈까스"))
    )

    @Test
    @DisplayName("campus 미지정/알 수 없는 값이면 강촌(6486)으로 조회한다")
    fun `기본_캠퍼스는_강촌`() {
        whenever(cjFreshMealClient.fetchToday("6486")).thenReturn(completeMeals())

        assertEquals("gangchon", mealService.getTodayMeal(null).campus)
        assertEquals("gangchon", mealService.getTodayMeal("없는캠퍼스").campus)
    }

    @Test
    @DisplayName("을지 캠퍼스는 storeIdx 5924 로 조회한다")
    fun `을지_캠퍼스_storeIdx`() {
        whenever(cjFreshMealClient.fetchToday("5924")).thenReturn(completeMeals())

        val result = mealService.getTodayMeal("eulji")

        assertEquals("eulji", result.campus)
        assertEquals("제육볶음", result.meals.lunch.first().name)
        verify(cjFreshMealClient).fetchToday("5924")
    }

    @Test
    @DisplayName("완료된 당일 식단은 캐시로 응답해 프레시밀을 다시 호출하지 않는다")
    fun `완료_식단은_당일_캐시`() {
        whenever(cjFreshMealClient.fetchToday("6486")).thenReturn(completeMeals())

        val first = mealService.getTodayMeal("gangchon")
        val second = mealService.getTodayMeal("gangchon")

        assertFalse(first.cached)
        assertTrue(second.cached)
        assertFalse(second.partial)
        verify(cjFreshMealClient, times(1)).fetchToday("6486")
    }

    @Test
    @DisplayName("force=true 면 캐시를 무시하고 다시 조회한다")
    fun `force_는_캐시_무시`() {
        whenever(cjFreshMealClient.fetchToday("6486")).thenReturn(completeMeals())

        mealService.getTodayMeal("gangchon")
        val refreshed = mealService.getTodayMeal("gangchon", force = true)

        assertFalse(refreshed.cached)
        verify(cjFreshMealClient, times(2)).fetchToday("6486")
    }

    @Test
    @DisplayName("중식 사진이 아직 안 올라온 식단은 partial=true 로 알려준다")
    fun `사진_미등록은_partial`() {
        whenever(cjFreshMealClient.fetchToday("6486")).thenReturn(
            MealsDto(morning = emptyList(), lunch = listOf(item("제육볶음", thumbnail = "")), dinner = emptyList())
        )

        assertTrue(mealService.getTodayMeal("gangchon").partial)
    }

    @Test
    @DisplayName("재조회가 기존 캐시보다 덜 채워졌으면 기존 캐시를 유지한다")
    fun `덜_채워진_재조회는_캐시_유지`() {
        whenever(cjFreshMealClient.fetchToday("6486"))
            .thenReturn(completeMeals())
            .thenReturn(MealsDto.EMPTY) // 프레시밀이 일시적으로 빈 응답

        mealService.getTodayMeal("gangchon")
        val refreshed = mealService.getTodayMeal("gangchon", force = true)

        assertEquals("제육볶음", refreshed.meals.lunch.first().name)
    }

    @Test
    @DisplayName("조회 실패 시 당일 캐시가 있으면 캐시로 응답한다")
    fun `조회_실패시_캐시_폴백`() {
        whenever(cjFreshMealClient.fetchToday("6486"))
            .thenReturn(completeMeals())
            .thenThrow(IllegalStateException("식단 조회에 실패했습니다."))

        mealService.getTodayMeal("gangchon")
        val fallback = mealService.getTodayMeal("gangchon", force = true)

        assertTrue(fallback.cached)
        assertEquals("제육볶음", fallback.meals.lunch.first().name)
    }

    @Test
    @DisplayName("캐시도 없고 조회도 실패하면 예외를 던진다")
    fun `캐시_없고_실패시_예외`() {
        whenever(cjFreshMealClient.fetchToday("6486")).thenThrow(IllegalStateException("식단 조회에 실패했습니다."))

        assertThrows<IllegalStateException> { mealService.getTodayMeal("gangchon") }
    }
}
