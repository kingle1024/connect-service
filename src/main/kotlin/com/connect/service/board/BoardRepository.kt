package com.connect.service.board

import com.connect.service.comment.BaseEntity
import jakarta.persistence.* // <- 임포트 확인!
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Entity
@Table(name = "board_mst")
data class BoardMst(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val title: String,
    val content: String,
    val author: String,
    var viewCount: Long = 0,
    var isDeleted: Boolean = false,
) : BaseEntity()

@Repository
interface BoardRepository : JpaRepository<BoardMst, Long> {

}
