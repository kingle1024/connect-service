package com.connect.service.user.domain

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class CustomUserDetails(
    private val user: Users // 실제 사용자 엔티티를 주입받아 사용
) : UserDetails {

    // Users 엔티티에 roles 필드가 Collection<Role> 또는 유사한 형태로 존재한다고 가정
    // roles가 null일 경우를 대비해 안전하게 처리 (예: Elvis operator)
    override fun getAuthorities(): Collection<GrantedAuthority> =
        user.roles?.map { SimpleGrantedAuthority(it.name) } ?: emptyList() // roles가 없다면 빈 리스트 반환

    // Users 엔티티의 'rawPassword' 필드를 사용
    override fun getPassword(): String = user.rawPassword

    // Users 엔티티의 'userId' 필드를 사용 (JWT subject와 CustomUserDetailsService의 loadUserByUsername과 일치시켜야 함)
    override fun getUsername(): String = user.userId // 혹은 user.name, JWT subject에 따라 다름

    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = true

    // 추가 정보 getter는 기존과 동일
    fun getUserId(): String = user.userId // user.id는 Long? 타입이므로, String으로 필요한 경우 적절히 변환
    fun getEmail(): String = user.email
    fun getName(): String = user.name
    fun getProfileUrl(): String? = user.profileUrl
}
