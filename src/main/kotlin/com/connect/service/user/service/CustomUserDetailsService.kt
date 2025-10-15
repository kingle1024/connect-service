package com.connect.service.user.service

import com.connect.service.user.domain.CustomUserDetails
import com.connect.service.user.domain.Users
import com.connect.service.user.repository.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        // 1. userRepository를 통해 Users 엔티티를 조회합니다.
        val user: Users = userRepository.findByUserId(username)
            ?: throw UsernameNotFoundException("User not found with userId: $username")

        return CustomUserDetails(user)
    }
}
