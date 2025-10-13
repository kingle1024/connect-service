package com.connect.service.common.security

import com.connect.service.common.jwt.JwtAuthenticationFilter
import com.connect.service.common.jwt.JwtTokenProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtTokenProvider: JwtTokenProvider
) {

    // 비밀번호 암호화에 사용할 PasswordEncoder 빈 등록
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    // AuthenticationManager 빈 등록 (로그인 시 인증 처리)
    @Bean
    fun authenticationManager(authenticationConfiguration: AuthenticationConfiguration): AuthenticationManager {
        return authenticationConfiguration.authenticationManager
    }

    // 보안 필터 체인 설정
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() } // REST API에서는 CSRF 보호가 필요 없으므로 비활성화
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) } // JWT 사용 시 세션 사용 안 함 (Stateless)
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("/api/auth/**").permitAll() // 로그인, 회원가입 등 인증 관련 경로는 모두 허용
                    .requestMatchers("/api/public/**").permitAll() // 공개 API 경로
                    .requestMatchers("/api/boards/**").permitAll() // 게시판 API 경로
                    .anyRequest().authenticated() // 그 외 모든 요청은 인증 필요
            }
            .headers { headers ->
                headers.frameOptions { it.sameOrigin() } // H2 콘솔을 위한 설정 (sameOrigin 설정)
            }
            // JWT 필터를 UsernamePasswordAuthenticationFilter 이전에 추가
            .addFilterBefore(JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}
