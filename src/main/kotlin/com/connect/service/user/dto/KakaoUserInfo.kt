package com.connect.service.user.dto

// 카카오 사용자 정보 응답에서 우리 서비스가 사용하는 값만 추린 DTO
data class KakaoUserInfo(
    val id: String, // 카카오 회원번호 (고유 식별자)
    val email: String?, // 카카오 계정 이메일 (동의 안 했으면 null)
    val nickname: String?, // 카카오 프로필 닉네임
    val profileImage: String? // 카카오 프로필 이미지 URL
)
