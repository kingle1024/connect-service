package com.connect.service.meal.client

import com.connect.service.meal.dto.MealItemDto
import com.connect.service.meal.dto.MealsDto
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import mu.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.time.Duration

private val logger = KotlinLogging.logger {}

/**
 * CJ프레시밀(구내식당) 오늘 식단 조회 클라이언트.
 *
 * 웹/앱에서 프레시밀을 직접 호출하면 CORS 로 막히므로 서버가 대신 받아서 내려준다.
 * 첫 호출이 콜드 커넥션으로 매달릴 때가 있어 타임아웃을 걸고 짧게 재시도한다.
 */
@Component
class CjFreshMealClient {

    private val restTemplate = RestTemplate(
        SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(Duration.ofSeconds(3))
            setReadTimeout(Duration.ofSeconds(6))
        }
    )

    fun fetchToday(storeIdx: String): MealsDto {
        var lastError: Exception? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                return fetchOnce(storeIdx)
            } catch (e: Exception) {
                lastError = e
                logger.warn { "식단 조회 실패(storeIdx=$storeIdx, ${attempt + 1}/$MAX_ATTEMPTS): ${e.message}" }
                if (attempt < MAX_ATTEMPTS - 1) Thread.sleep(RETRY_DELAY_MS)
            }
        }
        throw IllegalStateException("식단 조회에 실패했습니다.", lastError)
    }

    private fun fetchOnce(storeIdx: String): MealsDto {
        val request = RequestEntity
            .get(URI.create("$BASE_URL?storeIdx=$storeIdx"))
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build()
        val body = restTemplate.exchange(request, FreshMealApiResponse::class.java).body
        val data = body?.data ?: emptyMap()
        // mealCd 1=조식, 2=중식, 3=석식
        return MealsDto(
            morning = data.pick("1"),
            lunch = data.pick("2"),
            dinner = data.pick("3")
        )
    }

    private fun Map<String, List<FreshMealItem>>.pick(mealCd: String): List<MealItemDto> =
        (this[mealCd] ?: emptyList()).map {
            MealItemDto(
                corner = it.corner ?: "",
                name = it.name ?: "",
                side = it.side ?: "",
                kcal = it.kcal?.toInt() ?: 0,
                thumbnailUrl = it.thumbnailUrl ?: ""
            )
        }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class FreshMealApiResponse(
        val data: Map<String, List<FreshMealItem>>? = null
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class FreshMealItem(
        val corner: String? = null,
        val name: String? = null,
        val side: String? = null,
        val kcal: Double? = null,
        val thumbnailUrl: String? = null
    )

    companion object {
        private const val BASE_URL = "https://front.cjfreshmeal.co.kr/meal/v1/today-all-meal"
        private const val MAX_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 500L
    }
}
