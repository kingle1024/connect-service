package com.connect.service.inquiry.entity

import com.connect.service.comment.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 앱에서 접수한 개선/버그 요청(문의).
 *
 * 예전에는 드로어의 "개선/버그 요청" 메뉴가 mailto 로 메일 앱을 열었지만,
 * 이제는 이 테이블에 저장하고 백오피스에서 답변을 달아 앱에서 다시 확인할 수 있게 한다.
 *
 * 작성자는 userId(로그인 아이디) 문자열로만 들고 있고 users 테이블과 FK 를 걸지 않는다.
 * (board_mst 가 작성자를 다루는 방식과 동일)
 *
 * 로그인 없이도 접수할 수 있어서 작성자는 둘 중 하나다.
 *  - 회원   : userId 가 채워지고 guestKey 는 null
 *  - 비회원 : guestKey(단말이 만들어 캐시에 보관하는 임의 키)가 채워지고 userId 는 null
 * 비회원은 이 guestKey 로만 자기 문의를 다시 찾아볼 수 있다.
 */
@Entity
@Table(name = "inquiry_mst")
data class InquiryMst(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    // 비회원 문의면 null
    @Column(length = 100)
    val userId: String? = null,

    // 비회원이 자기 문의 내역을 다시 조회할 때 쓰는 단말 키. 회원 문의면 null
    @Column(length = 64)
    val guestKey: String? = null,

    // 비회원이 답변을 메일로 받고 싶을 때만 남기는 선택 입력값
    @Column(length = 255)
    var guestEmail: String? = null,

    // 작성 시점의 이름. 이후 사용자가 이름을 바꿔도 접수 당시 이름을 유지한다.
    @Column(nullable = false, length = 100)
    val userName: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var type: InquiryType,

    @Column(nullable = false, length = 200)
    var title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: InquiryStatus = InquiryStatus.RECEIVED,

    // 관리자 답변. 아직 답변이 없으면 null 이다.
    @Column(columnDefinition = "TEXT")
    var answer: String? = null,

    // 답변한 관리자의 로그인 아이디
    @Column(length = 100)
    var answeredBy: String? = null,

    var answeredDts: LocalDateTime? = null,

    // 관리자가 백오피스에서 숨긴 문의. 사용자 목록에서도 빠진다.
    @Column(nullable = false)
    var isDeleted: Boolean = false
) : BaseEntity()
