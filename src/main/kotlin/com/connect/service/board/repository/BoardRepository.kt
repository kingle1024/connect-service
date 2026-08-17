package com.connect.service.board.repository

import com.connect.service.board.entity.BoardMst
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

@Repository
interface BoardRepository : JpaRepository<BoardMst, Long>, BoardRepositoryCustom {
    // 모집 탭 목록/검색은 BoardRepositoryCustom.searchBoards 를 사용한다

    // 필터 칩에 노출할 카테고리 목록. 직접입력 카테고리도 있으므로 실제 데이터에서 뽑는다.
    @Query("SELECT DISTINCT b.category FROM BoardMst b WHERE b.isDeleted = false ORDER BY b.category ASC")
    fun findDistinctCategories(): List<String>

    // 특정 작성자(userId)의 삭제되지 않은 게시글 목록 (마이페이지 - 내가 쓴 글)
    fun findAllByUserIdAndIsDeletedFalse(userId: String, pageable: Pageable): Page<BoardMst>

    // 작성자(userId)의 게시글 작성자명을 일괄 변경 (이름 변경 시 기존 글도 함께 반영)
    @Modifying
    @Query("UPDATE BoardMst b SET b.userName = :userName WHERE b.userId = :userId")
    fun updateUserNameByUserId(@Param("userId") userId: String, @Param("userName") userName: String): Int

    // 카테고리별 게시글 수 (백오피스 카테고리 목록에서 삭제 가능 여부 판단)
    @Query(
        """
        SELECT b.category AS category, COUNT(b) AS boardCount
        FROM BoardMst b
        WHERE b.isDeleted = false
        GROUP BY b.category
        """
    )
    fun countGroupByCategory(): List<CategoryBoardCount>

    // 특정 카테고리를 쓰는 게시글 수
    fun countByCategoryAndIsDeletedFalse(category: String): Long

    // 카테고리 이름 변경 시 기존 게시글의 category 문자열도 함께 갱신 (board_mst 는 FK 없이 이름을 들고 있다)
    @Modifying
    @Query("UPDATE BoardMst b SET b.category = :newName WHERE b.category = :oldName")
    fun updateCategoryName(@Param("oldName") oldName: String, @Param("newName") newName: String): Int
}
