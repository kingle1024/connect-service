package com.connect.service.user.repository

import com.connect.service.user.domain.UserRole
import com.connect.service.user.domain.Users
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface UserRepositoryCustom {
    /**
     * 백오피스 회원 검색. 조건은 모두 선택이며 null 이면 해당 조건을 걸지 않는다.
     *
     * @param keyword 로그인 아이디/이름/이메일 부분 일치
     */
    fun searchForAdmin(
        keyword: String?,
        role: UserRole?,
        enabled: Boolean?,
        pageable: Pageable
    ): Page<Users>

    // ROLE_ADMIN 보유 회원 수 (마지막 관리자 권한 회수를 막기 위한 확인용)
    fun countByRole(role: UserRole): Long
}
