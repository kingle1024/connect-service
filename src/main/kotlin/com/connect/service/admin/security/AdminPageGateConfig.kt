package com.connect.service.admin.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
class AdminPageGateConfig {

    // 백오피스 화면 파일에만 건다. /api/admin/** 은 Bearer 토큰을 쓰므로 여기서 건드리지 않는다.
    // 스프링 시큐리티 필터 체인보다 먼저 돈다.
    @Bean
    fun adminPageGateFilter(
        @Value("\${app.admin.page-password-hash:}") passwordHash: String
    ): FilterRegistrationBean<AdminPageGateFilter> =
        FilterRegistrationBean(AdminPageGateFilter(passwordHash)).apply {
            addUrlPatterns("/admin", "/admin/*")
            order = Ordered.HIGHEST_PRECEDENCE
        }
}
