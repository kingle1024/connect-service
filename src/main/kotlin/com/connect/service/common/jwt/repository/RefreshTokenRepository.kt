package com.connect.service.common.jwt.repository

import com.connect.service.common.jwt.dto.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByToken(token: String): RefreshToken?
    fun findByUserIdAndIsRevokedFalse(userId: String): List<RefreshToken> // 특정 사용자의 유효한 Refresh Token 목록 조회
    fun findByUserIdAndToken(userId: String, token: String): RefreshToken?
}
