package com.connect.service.common.jwt

import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Component
import java.security.Key
import java.util.*

@Component
class JwtTokenProvider(
    private val userDetailsService: UserDetailsService // Spring Security의 UserDetailsService 주입
) {

    @Value("\${jwt.secret-key}") // application.properties에서 secret-key를 주입받습니다.
    private lateinit var secretKeyString: String
    private lateinit var secretKey: Key

    @Value("\${jwt.access-token-expiration-milliseconds}")
    private var accessTokenExpirationMs: Long = 0

    @Value("\${jwt.refresh-token-expiration-milliseconds}")
    private var refreshTokenExpirationMs: Long = 0

    // 객체 생성 후 secretKey를 Base64 디코딩하여 사용
    @PostConstruct
    protected fun init() {
        secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretKeyString))
    }

    // Access Token 생성
    fun createAccessToken(authentication: Authentication): String {
        val userPrincipal = authentication.principal as UserDetails
        val claims = Jwts.claims().setSubject(userPrincipal.username) // Subject는 userId
        claims["roles"] = userPrincipal.authorities.map { it.authority } // 사용자 권한 정보

        val now = Date()
        val validity = Date(now.time + accessTokenExpirationMs)

        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact()
    }

    // Refresh Token 생성 (일반적으로 클레임 없이 더 긴 만료 시간)
     fun createRefreshToken(authentication: Authentication): Pair<String, Date> {
        val userPrincipal = authentication.principal as UserDetails
        val now = Date()
        val validity = Date(now.time + refreshTokenExpirationMs)

        val refreshToken = Jwts.builder()
            .setSubject(userPrincipal.username)
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact()

        return Pair(refreshToken, validity) // 토큰 문자열과 만료 시각 반환
    }


    // JWT에서 인증 정보 조회
    fun getAuthentication(token: String): Authentication {
        val username = Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).body.subject
        val userDetails = userDetailsService.loadUserByUsername(username)
        return UsernamePasswordAuthenticationToken(userDetails, "", userDetails.authorities)
    }

    // JWT 유효성 검증
    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token)
            true
        } catch (e: SecurityException) {
            println("Invalid JWT signature.")
            false
        } catch (e: MalformedJwtException) {
            println("Invalid JWT token.")
            false
        } catch (e: ExpiredJwtException) {
            println("Expired JWT token.")
            false
        } catch (e: UnsupportedJwtException) {
            println("Unsupported JWT token.")
            false
        } catch (e: IllegalArgumentException) {
            println("JWT claims string is empty.")
            false
        }
    }

    // 토큰에서 사용자 아이디 추출 (주로 refresh 토큰 검증 시 사용)
    fun getUserIdFromToken(token: String): String {
        return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).body.subject
    }

    // 토큰에서 만료 시각 추출
    fun getExpirationDateFromToken(token: String): Date {
        return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).body.expiration
    }
}
