package com.connect.service.user.repository

import com.connect.service.user.domain.Users
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<Users, Long> {
    fun findByUserId(userId: String): Users?
}
