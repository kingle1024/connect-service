package com.connect.service.comment

import jakarta.persistence.*

@Entity
@Table(name = "comment_mst") // 메인 댓글 테이블 이름
data class CommentMst(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 255)
    val userId: String, // 댓글 작성자 ID

    @Column(nullable = false, length = 100)
    val userName: String, // 댓글 작성자 이름

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String, // 댓글 내용

    @Column(nullable = false)
    val postId: Long, // 연결된 게시글 ID

    var isDeleted: Boolean = false, // 댓글 삭제 여부

    @OneToMany(mappedBy = "parentComment", cascade = [CascadeType.ALL], orphanRemoval = true)
    val replies: MutableList<CommentDtl> = mutableListOf()
) : BaseEntity()
