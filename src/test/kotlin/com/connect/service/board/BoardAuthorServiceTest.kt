package com.connect.service.board

import com.connect.service.board.entity.BoardMst
import com.connect.service.board.repository.BoardRepository
import com.connect.service.board.service.BoardService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExtendWith(MockitoExtension::class)
class BoardAuthorServiceTest {

    @Mock
    private lateinit var boardRepository: BoardRepository

    @InjectMocks
    private lateinit var boardService: BoardService

    private fun sampleBoard(id: Long, userId: String) = BoardMst(
        id = id,
        title = "제목$id",
        content = "내용$id",
        category = "기타",
        userId = userId,
        userName = "홍길동",
        deadlineDts = LocalDateTime.now().plusDays(1),
        destination = "강변역",
        maxCapacity = 4,
        currentParticipants = 1
    )

    @Test
    @DisplayName("작성자의 게시글을 DTO로 변환해 반환하고, 마지막 페이지면 nextPageToken은 null")
    fun `작성자_게시글_조회`() {
        // Given
        val pageable: Pageable = PageRequest.of(0, 10)
        val boards = listOf(sampleBoard(1, "kakao_1"), sampleBoard(2, "kakao_1"))
        whenever(boardRepository.findAllByUserIdAndIsDeletedFalse(eq("kakao_1"), any()))
            .thenReturn(PageImpl(boards, pageable, boards.size.toLong()))

        // When
        val result = boardService.getBoardsByAuthor("kakao_1", pageable)

        // Then
        assertEquals(2, result.posts.size)
        assertEquals("kakao_1", result.posts[0].userId)
        assertNull(result.nextPageToken) // 더 이상 페이지 없음
    }

    @Test
    @DisplayName("다음 페이지가 있으면 nextPageToken은 다음 페이지 번호다")
    fun `다음_페이지_토큰`() {
        // Given: 전체 2개, 페이지 크기 1 → 다음 페이지 존재
        val pageable: Pageable = PageRequest.of(0, 1)
        val boards = listOf(sampleBoard(1, "kakao_1"))
        whenever(boardRepository.findAllByUserIdAndIsDeletedFalse(eq("kakao_1"), any()))
            .thenReturn(PageImpl(boards, pageable, 2L))

        // When
        val result = boardService.getBoardsByAuthor("kakao_1", pageable)

        // Then
        assertEquals(1, result.nextPageToken)
    }
}
