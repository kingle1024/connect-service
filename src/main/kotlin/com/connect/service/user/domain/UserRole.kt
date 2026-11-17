package com.connect.service.user.domain

enum class UserRole {
    ROLE_USER,
    ROLE_ADMIN,
    ROLE_VERIFIED, // 더존 이메일 인증 완료 사용자 (인증 마크 표시)
    // 로그인하지 않은 방문자에게 자동으로 만들어 주는 게스트 계정.
    // 비밀번호로는 로그인할 수 없고, 서버가 발급한 게스트 쿠키로만 본인임이 확인된다.
    ROLE_GUEST
}
