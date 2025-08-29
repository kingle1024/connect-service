package com.connect.service.common

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@Configuration // 이 클래스가 스프링 설정 클래스임을 명시
@EnableJpaAuditing // 여기서 JPA Auditing 기능을 활성화
class JpaAuditingConfig {
}
