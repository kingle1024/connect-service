package com.connect.service.comment

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "comment_dtl") // 대댓글 테이블 이름
data class CommentDtl(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 255)
    val userId: String, // 대댓글 작성자 ID

    @Column(nullable = false, length = 100)
    val userName: String, // 대댓글 작성자 이름

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String, // 대댓글 내용

    @Column(nullable = false)
    val parentId: Long, // 부모 댓글 ID (CommentMst의 id를 참조)

    @ManyToOne(fetch = FetchType.LAZY) // LAZY 로딩으로 성능 최적화 (필요할 때만 로드)
    @JoinColumn(name = "parentId", insertable = false, updatable = false) // FK가 parentId 컬럼임을 명시
    // insertable = false, updatable = false: parentId 필드는 FK 역할을 하지만,
    // 실제로는 CommentDtl 엔티티 내부의 parentId 변수가 값을 가지고 있으므로,
    // JPA가 이 필드를 직접 INSERT/UPDATE하지 않도록 설정해두는 게 충돌을 막아.
    val parentComment: CommentMst? = null, // 부모 댓글 엔티티 (null 허용)

    var isDeleted: Boolean = false, // 대댓글 삭제 여부
) : BaseEntity()
