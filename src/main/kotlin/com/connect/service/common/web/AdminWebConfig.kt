package com.connect.service.common.web

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * 백오피스 정적 화면 라우팅.
 *
 * Spring Boot 는 static/admin/index.html 을 /admin/index.html 로만 서빙하므로
 * /admin, /admin/ 으로 들어와도 같은 화면이 열리도록 포워딩한다.
 */
@Configuration
class AdminWebConfig : WebMvcConfigurer {

    override fun addViewControllers(registry: ViewControllerRegistry) {
        registry.addViewController("/admin").setViewName("forward:/admin/index.html")
        registry.addViewController("/admin/").setViewName("forward:/admin/index.html")
    }
}
