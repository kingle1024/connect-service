package com.connect.service.board

import jakarta.persistence.* // <- 임포트 확인!
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

// DB의 "boards" 테이블과 맵핑될 데이터 클래스 (엔티티)
@Entity
@Table(name = "boards") // 테이블 이름도 boards로 변경!
data class Board( // 클래스 이름도 Board로 변경!
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val content: String // 게시글 내용
)

@Repository
interface BoardRepository : JpaRepository<Board, Long> { // Board 엔티티에 맞춰 변경!
    // JpaRepository는 여전히 잘 작동해!
}
