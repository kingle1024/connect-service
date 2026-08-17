package com.connect.service.user.repository.impl

import com.connect.service.user.domain.QUsers
import com.connect.service.user.domain.UserRole
import com.connect.service.user.domain.Users
import com.connect.service.user.repository.UserRepositoryCustom
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : UserRepositoryCustom {

    private val user = QUsers.users

    override fun searchForAdmin(
        keyword: String?,
        role: UserRole?,
        enabled: Boolean?,
        pageable: Pageable
    ): Page<Users> {
        val where = BooleanBuilder()

        keyword?.trim()?.takeIf { it.isNotEmpty() }?.let { kw ->
            where.and(
                user.userId.containsIgnoreCase(kw)
                    .or(user.name.containsIgnoreCase(kw))
                    .or(user.email.containsIgnoreCase(kw))
            )
        }
        // roles 는 @ElementCollection 이므로 any() 로 컬렉션 내 존재 여부를 본다.
        role?.let { where.and(user.roles.any().eq(it)) }
        enabled?.let { where.and(user.enabled.eq(it)) }

        val content = queryFactory
            .selectFrom(user)
            .where(where)
            .orderBy(user.id.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory
            .select(user.count())
            .from(user)
            .where(where)
            .fetchOne() ?: 0L

        return PageImpl(content, pageable, total)
    }

    override fun countByRole(role: UserRole): Long =
        queryFactory
            .select(user.count())
            .from(user)
            .where(user.roles.any().eq(role))
            .fetchOne() ?: 0L
}
