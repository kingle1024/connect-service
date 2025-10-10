package com.connect.service.user.domain

import jakarta.persistence.*
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

@Entity
@Table(name = "users")
data class Users(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(unique = true, nullable = false)
    val userId: String, // 로그인 시 사용될 사용자 ID

    @Column(nullable = false)
    val email: String, // 더존 이메일

    @Column(nullable = false)
    val name: String, // 사용자 별칭

    @Column(name = "password", nullable = false)
    var rawPassword: String, // 비밀번호 (암호화하여 저장)

    val profileUrl: String? = null,

    // 계정의 권한 목록
    @ElementCollection(fetch = FetchType.EAGER) // 즉시 로딩
    @CollectionTable(name = "user_roles", joinColumns = [JoinColumn(name = "user_id")])
    @Enumerated(EnumType.STRING)
    var roles: MutableSet<UserRole> = mutableSetOf(UserRole.ROLE_USER)
) : UserDetails {

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> =
        roles.map { SimpleGrantedAuthority(it.name) }.toMutableList()
    override fun getPassword(): String = rawPassword
    override fun getUsername(): String = userId // Spring Security에서 사용자명으로 사용
    override fun isAccountNonExpired(): Boolean = true // 계정 만료 여부
    override fun isAccountNonLocked(): Boolean = true // 계정 잠금 여부
    override fun isCredentialsNonExpired(): Boolean = true // 비밀번호 만료 여부
    override fun isEnabled(): Boolean = true // 계정 활성화 여부
}
