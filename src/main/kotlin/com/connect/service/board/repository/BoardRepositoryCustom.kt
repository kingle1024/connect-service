package com.connect.service.board.repository

import com.connect.service.board.entity.BoardMst
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface BoardRepositoryCustom {
    /**
     * 백오피스 게시물 검색. 조건은 모두 선택이며 null 이면 해당 조건을 걸지 않는다.
     *
     * @param keyword 제목/내용/작성자ID/작성자명 부분 일치
     * @param isDeleted null 이면 삭제 여부와 무관하게 전체 조회
     */
    fun searchForAdmin(
        keyword: String?,
        category: String?,
        isDeleted: Boolean?,
        pageable: Pageable
    ): Page<BoardMst>

    /**
     * 모집 탭 게시물 검색. 조건은 모두 선택이며 null/빈 값이면 해당 조건을 걸지 않는다.
     * 삭제된 글은 항상 제외한다.
     *
     * @param chipCategories 앱에 칩으로 노출 중인 카테고리 목록. '기타' 칩으로 조회할 때만 채워서 넘긴다.
     *        값이 있으면 category 조건이 "이름이 정확히 같거나, 이 목록에 없는(=칩이 없는) 카테고리" 로
     *        확장된다. (BoardService.getAllBoards 참고)
     * @param destination 장소(도착지) 부분 일치
     * @param title 제목 부분 일치
     * @param content 내용 부분 일치
     * @param openOnly true 면 모집 미완료(정원 미달이면서 마감 전)인 글만 조회
     */
    fun searchBoards(
        category: String?,
        chipCategories: List<String>?,
        destination: String?,
        title: String?,
        content: String?,
        openOnly: Boolean,
        pageable: Pageable
    ): Page<BoardMst>
}
