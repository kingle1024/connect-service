package com.connect.service.common.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * 전역 CORS 설정.
 *
 * 이전에는 컨트롤러마다 @CrossOrigin 을 달아 localhost 주소를 하드코딩해 두는 바람에
 * 배포된 프론트에서 호출하면 전부 CORS 로 막혔다. 이제 여기 한 곳에서만 관리한다.
 *
 * 프론트의 axios 가 withCredentials: true 로 호출하므로 allowCredentials 를 켜야 하고,
 * 그러면 Origin 에 "*" 를 쓸 수 없다. 그래서 와일드카드를 쓸 수 있는
 * allowedOriginPatterns 로 허용 목록을 지정한다.
 */
@Configuration
class CorsConfig(
    @Value("\${app.cors.allowed-origin-patterns}")
    private val originPatterns: List<String>
) {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration()
        config.allowedOriginPatterns = originPatterns
        config.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD")
        config.allowedHeaders = listOf("*")
        // 프론트가 응답 헤더에서 직접 읽어야 하는 값들
        config.exposedHeaders = listOf("Authorization", "Content-Disposition")
        config.allowCredentials = true
        // preflight 결과를 1시간 캐싱해서 OPTIONS 왕복을 줄인다
        config.maxAge = 3600L

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
    }
}
