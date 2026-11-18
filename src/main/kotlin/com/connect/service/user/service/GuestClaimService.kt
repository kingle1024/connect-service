package com.connect.service.user.service

import com.connect.service.user.domain.UserRole
import com.connect.service.user.repository.UserRepository
import jakarta.persistence.EntityManager
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

/**
 * 게스트로 쓴 기록을 회원 계정으로 가져오기.
 *
 * 로그인한 회원이 같은 기기의 게스트 신원(서명된 게스트 쿠키/토큰)을 보여주면
 * 그 게스트의 글·댓글·채팅방 참여·채팅 메시지를 회원 아이디로 옮기고, 게스트 계정은 비활성화한다.
 * 게스트 신원은 서버 서명이 있어야 하므로 남의 게스트 기록을 가져갈 수는 없다.
 * 한 번 가져가면 게스트가 비활성화돼 다른 계정이 또 가져갈 수 없다.
 *
 * 여러 테이블을 한꺼번에 바꿔야 해서 네이티브 UPDATE 를 한 트랜잭션으로 묶는다.
 */
@Service
class GuestClaimService(
    private val userRepository: UserRepository,
    private val guestService: GuestService,
    private val entityManager: EntityManager,
) {

    data class Summary(
        val available: Boolean,
        val guestUserId: String? = null,
        val guestName: String? = null,
        val boardCount: Long = 0,
        val commentCount: Long = 0,
        val chatRoomCount: Long = 0,
    ) {
        val total: Long get() = boardCount + commentCount + chatRoomCount
    }

    /** 가져올 수 있는 게스트 기록 요약. 게스트 신원이 없거나, 가져올 게 하나도 없으면 available=false. */
    @Transactional(readOnly = true)
    fun summary(memberUserId: String, guestUserId: String?): Summary {
        requireMember(memberUserId)
        val guest = guestUserId?.let { guestService.findActiveGuest(it) } ?: return Summary(available = false)

        val s = Summary(
            available = true,
            guestUserId = guest.userId,
            guestName = guest.name,
            boardCount = count("SELECT COUNT(*) FROM board_mst WHERE user_id = :g AND is_deleted = 0", guest.userId),
            commentCount = count("SELECT COUNT(*) FROM reply WHERE user_id = :g", guest.userId),
            chatRoomCount = count("SELECT COUNT(*) FROM room_memberships WHERE user_id = :g", guest.userId),
        )
        return if (s.total == 0L) Summary(available = false) else s
    }

    /** 게스트 기록을 회원 계정으로 옮긴다. 옮긴 건수를 돌려준다. */
    @Transactional
    fun claim(memberUserId: String, guestUserId: String?): Summary {
        val member = requireMember(memberUserId)
        val guest = guestUserId?.let { guestService.findActiveGuest(it) }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "가져올 게스트 기록이 없습니다.")
        val before = summary(memberUserId, guest.userId)

        val g = guest.userId
        val m = member.userId

        // 글·댓글: 작성자 아이디와 표시 이름을 회원 것으로
        update("UPDATE board_mst SET user_id = :m, user_name = :name WHERE user_id = :g", g, m, member.name)
        update("UPDATE reply SET user_id = :m, user_name = :name WHERE user_id = :g", g, m, member.name)

        // 1:1 방에서 상대방에게 보이는 방 이름은 게스트 닉네임이었다 → 회원 이름으로
        entityManager.createNativeQuery(
            """
            UPDATE room_memberships SET room_name = :name
            WHERE user_id <> :g AND room_name = :guestName
              AND room_id IN (SELECT room_id FROM (SELECT room_id FROM room_memberships WHERE user_id = :g) t)
            """.trimIndent()
        ).setParameter("name", member.name).setParameter("guestName", guest.name).setParameter("g", g).executeUpdate()

        // 방 참여: 회원이 이미 있는 방은 게스트 참여만 지우고, 나머지는 회원으로 바꾼다 (PK = user_id + room_id)
        update(
            """
            UPDATE room_memberships SET user_id = :m
            WHERE user_id = :g
              AND room_id NOT IN (SELECT room_id FROM (SELECT room_id FROM room_memberships WHERE user_id = :m) t)
            """.trimIndent(), g, m
        )
        update("DELETE FROM room_memberships WHERE user_id = :g", g, m)
        update("UPDATE chat_rooms SET leader_user_id = :m WHERE leader_user_id = :g", g, m)

        // 이미 보낸 메시지도 내 것으로 보이게 보낸 사람/받는 사람을 바꾼다
        update("UPDATE chat_messages SET sender = :m WHERE sender = :g", g, m)
        update("UPDATE chat_messages SET recipient = :m WHERE recipient = :g", g, m)

        // 게스트 계정은 비활성화 → 같은 기록을 다른 계정이 또 가져갈 수 없고, 이 기기는 다음부터 새 게스트가 된다
        guest.enabled = false
        userRepository.save(guest)

        return before
    }

    private fun requireMember(memberUserId: String) =
        userRepository.findByUserId(memberUserId)
            ?.takeIf { !it.roles.contains(UserRole.ROLE_GUEST) }
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.")

    private fun count(sql: String, guestUserId: String): Long =
        (entityManager.createNativeQuery(sql).setParameter("g", guestUserId).singleResult as Number).toLong()

    private fun update(sql: String, g: String, m: String, name: String? = null): Int {
        val q = entityManager.createNativeQuery(sql).setParameter("g", g)
        if (sql.contains(":m")) q.setParameter("m", m)
        if (name != null) q.setParameter("name", name)
        return q.executeUpdate()
    }
}
