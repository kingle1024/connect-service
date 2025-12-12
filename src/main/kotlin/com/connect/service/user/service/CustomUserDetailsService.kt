package com.connect.service.user.service

import com.connect.service.user.dto.CustomUserDto
import com.connect.service.user.domain.CustomUserDetails
import com.connect.service.user.domain.Users
import com.connect.service.user.repository.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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

    @Transactional(readOnly = true) // 읽기 전용 트랜잭션으로 설정하여 성능 최적화
    fun getRoomMemberShipByUserIds(userIds: List<String>): Map<String, CustomUserDto> {
        // 1. 주어진 userIds 목록으로 RoomMembership 엔티티들을 모두 조회합니다.
        val users = userRepository.findByUserIdIn(userIds)

        val map: Map<String, CustomUserDto> = users.associate { membership ->
            // 키(Key)는 RoomMembershipId.userId
            val key = membership.userId

            // 값(Value)은 RoomMembership 엔티티를 RoomMembershipDto로 변환
            val value = CustomUserDto(
                userId = membership.userId,
                email = membership.email,
                name = membership.name
            )
            key to value
        }

        return map
    }
}
