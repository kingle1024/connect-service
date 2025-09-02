package com.connect.service.comment

import com.connect.service.board.BoardMst
import com.connect.service.board.BoardRepository
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.Mockito
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDateTime
import java.util.Optional;

@ExtendWith(MockitoExtension::class)
class CommentServiceTest {

    @Mock // Mock 객체 생성 (Mockito)
    private lateinit var commentRepository: CommentRepository

    @Mock // Mock 객체 생성 (Mockito)
    private lateinit var boardRepository: BoardRepository

    // Mock 객체들을 주입받아 테스트할 서비스 인스턴스 생성 (Mockito)
    @InjectMocks
    private lateinit var commentService: CommentService

    @BeforeEach // 각 테스트 시작 전 초기화
    fun setUp() {
    }

    @Nested
    @DisplayName("댓글 생성 (createComment) 테스트")
    inner class CreateCommentTests {

        @Test
        @DisplayName("성공: 게시글이 존재하고 부모 댓글이 없는 경우")
        fun `createComment should create a comment when board exists and no parent comment`() {
            // Given
            val boardId = 1L
            val request = CreateCommentRequest(content = "새 댓글입니다.", author = "글쓴이")
            val board = BoardMst(id = boardId, title = "제목", content = "내용", author = "김코딩")
            val savedCommentId = 10L // 저장될 댓글의 ID 가정
            val now = LocalDateTime.now() // insertDts를 고정하기 위함

            // Mocking (given().willReturn() 사용 - BDD 스타일)
            // findByIdOrNull은 Kotlin 확장 함수라서 약간의 우회 필요
            `when`(boardRepository.findById(boardId)).thenReturn(Optional.of(board)) // findById를 직접 모킹

            // commentRepository.save(any<CommentMst>())에 대한 모킹
            // `any(CommentMst::class.java)`는 Kotlin 클래스를 Mockito `any()`에 전달할 때 사용
            given(commentRepository.save(any(CommentMst::class.java))).willAnswer { invocation ->
                val comment = invocation.getArgument(0, CommentMst::class.java)
                comment.copy(id = savedCommentId, insertDts = now) // 저장될 때 ID와 시간을 할당
            }

            // When
            val response = commentService.createComment(boardId, request)

            // Then
            assertNotNull(response)
            assertEquals(savedCommentId, response.id)
            assertEquals(request.content, response.content)
            assertEquals(request.author, response.author)
            assertEquals(boardId, response.boardId)
            assertNull(response.parentCommentId)
            assertEquals(now, response.insertDts) // 고정된 시간으로 검증

            // 검증 (verify() 사용)
            verify(boardRepository, times(1)).findById(boardId)
            verify(commentRepository, times(1)).save(any(CommentMst::class.java))
            verify(commentRepository, times(0)).findById(any(Long::class.java)) // 부모 댓글 조회가 없어야 함
        }

        @Test
        @DisplayName("성공: 게시글이 존재하고 유효한 부모 댓글이 있는 경우")
        fun `createComment should create a comment when board and parent comment exist`() {
            // Given
            val boardId = 1L
            val parentCommentId = 100L
            val request = CreateCommentRequest(content = "대댓글입니다.", author = "대댓글러", parentCommentId = parentCommentId)
            val board = BoardMst(id = boardId, title = "제목", content = "내용", author = "김코딩")
            val parentComment = CommentMst(id = parentCommentId, content = "부모 댓글", author = "원글쓴이", boardId = boardId)
            val savedCommentId = 11L
            val now = LocalDateTime.now()

            // Mocking
            `when`(boardRepository.findById(boardId)).thenReturn(java.util.Optional.of(board))
            `when`(commentRepository.findById(parentCommentId)).thenReturn(java.util.Optional.of(parentComment))
            given(commentRepository.save(any(CommentMst::class.java))).willAnswer { invocation ->
                val comment = invocation.getArgument(0, CommentMst::class.java)
                comment.copy(id = savedCommentId, insertDts = now)
            }

            // When
            val response = commentService.createComment(boardId, request)

            // Then
            assertNotNull(response)
            assertEquals(savedCommentId, response.id)
            assertEquals(request.content, response.content)
            assertEquals(request.author, response.author)
            assertEquals(boardId, response.boardId)
            assertEquals(parentCommentId, response.parentCommentId)
            assertEquals(now, response.insertDts)

            verify(boardRepository, times(1)).findById(boardId)
            verify(commentRepository, times(1)).findById(parentCommentId)
            verify(commentRepository, times(1)).save(any(CommentMst::class.java))
        }

        @Test
        @DisplayName("실패: 게시글이 존재하지 않는 경우 IllegalArgumentException 발생")
        fun `createComment should throw IllegalArgumentException when board does not exist`() {
            // Given
            val boardId = 999L
            val request = CreateCommentRequest(content = "새 댓글입니다.", author = "글쓴이")

            // Mocking
            `when`(boardRepository.findById(boardId)).thenReturn(java.util.Optional.empty()) // 게시글이 없는 경우

            // When & Then
            val exception = assertThrows<IllegalArgumentException> {
                commentService.createComment(boardId, request)
            }
            assertEquals("게시글을 찾을 수 없습니다: $boardId", exception.message)

            verify(boardRepository, times(1)).findById(boardId)
            verify(commentRepository, times(0)).save(any(CommentMst::class.java))
            verify(commentRepository, times(0)).findById(any(Long::class.java))
        }

        @Test
        @DisplayName("실패: 부모 댓글이 존재하지 않는 경우 IllegalArgumentException 발생")
        fun `createComment should throw IllegalArgumentException when parent comment does not exist`() {
            // Given
            val boardId = 1L
            val parentCommentId = 999L
            val request = CreateCommentRequest(content = "대댓글입니다.", author = "대댓글러", parentCommentId = parentCommentId)
            val board = BoardMst(id = boardId, title = "제목", content = "내용", author = "김코딩")

            // Mocking
            `when`(boardRepository.findById(boardId)).thenReturn(java.util.Optional.of(board))
            `when`(commentRepository.findById(parentCommentId)).thenReturn(java.util.Optional.empty()) // 부모 댓글이 없는 경우

            // When & Then
            val exception = assertThrows<IllegalArgumentException> {
                commentService.createComment(boardId, request)
            }
            assertEquals("부모 댓글을 찾을 수 없습니다: $parentCommentId", exception.message)

            verify(boardRepository, times(1)).findById(boardId)
            verify(commentRepository, times(1)).findById(parentCommentId)
            verify(commentRepository, times(0)).save(any(CommentMst::class.java))
        }
    }

    @Nested
    @DisplayName("댓글 조회 (getCommentsByBoardId) 테스트")
    inner class GetCommentsByBoardIdTests {

        @Test
        @DisplayName("성공: 댓글이 없는 경우 빈 리스트 반환")
        fun `getCommentsByBoardId should return empty list when no comments exist`() {
            // Given
            val boardId = 1L
            given(commentRepository.findAllByBoardIdOrderByInsertDtsAsc(boardId)).willReturn(emptyList())

            // When
            val response = commentService.getCommentsByBoardId(boardId)

            // Then
            assertTrue(response.isEmpty())
            verify(commentRepository, times(1)).findAllByBoardIdOrderByInsertDtsAsc(boardId)
        }

        @Test
        @DisplayName("성공: 최상위 댓글만 있는 경우 (계층 없음)")
        fun `getCommentsByBoardId should return flat list for top-level comments`() {
            // Given
            val boardId = 1L
            val now = LocalDateTime.now()
            val comment1 = CommentMst(id = 1L, content = "댓글1", author = "A", boardId = boardId, insertDts = now)
            val comment2 = CommentMst(id = 2L, content = "댓글2", author = "B", boardId = boardId, insertDts = now.plusMinutes(1))
            val comments = listOf(comment1, comment2)

            given(commentRepository.findAllByBoardIdOrderByInsertDtsAsc(boardId)).willReturn(comments)

            // When
            val response = commentService.getCommentsByBoardId(boardId)

            // Then
            assertEquals(2, response.size)
            assertEquals(1L, response[0].id)
            assertEquals(2L, response[1].id)
            assertTrue(response[0].replies.isEmpty())
            assertTrue(response[1].replies.isEmpty())
            verify(commentRepository, times(1)).findAllByBoardIdOrderByInsertDtsAsc(boardId)
        }

        @Test
        @DisplayName("성공: 대댓글이 포함된 계층 구조 조회")
        fun `getCommentsByBoardId should return hierarchical comments correctly`() {
            // Given
            val boardId = 1L
            val now = LocalDateTime.now()

            // 댓글과 대댓글 데이터
            val c1 = CommentMst(id = 1L, content = "댓글1", author = "A", boardId = boardId, insertDts = now)
            val c1_1 = CommentMst(id = 2L, content = "└대댓글1-1", author = "B", boardId = boardId, parentCommentId = 1L, insertDts = now.plusSeconds(1))
            val c1_2 = CommentMst(id = 3L, content = "└대댓글1-2", author = "C", boardId = boardId, parentCommentId = 1L, insertDts = now.plusSeconds(2))
            val c2 = CommentMst(id = 4L, content = "댓글2", author = "D", boardId = boardId, insertDts = now.plusMinutes(1))
            val c1_1_1 = CommentMst(id = 5L, content = "└└대댓글1-1-1", author = "E", boardId = boardId, parentCommentId = 2L, insertDts = now.plusSeconds(3))

            // 실제 DB에서 조회되는 순서 (insertDts asc)
            // 참고: Mockito의 `any()`는 null을 반환할 수 있으므로,
            // 이처럼 `findAllByBoardIdOrderByInsertDtsAsc`가 null을 반환하지 않도록
            // `java.util.Optional.of(value)`나 `value`를 직접 반환하도록 `given()`을 사용해야 해.
            val allComments = listOf(c1, c1_1, c1_2, c1_1_1, c2).sortedBy { it.insertDts }

            given(commentRepository.findAllByBoardIdOrderByInsertDtsAsc(boardId)).willReturn(allComments)

            // When
            val response = commentService.getCommentsByBoardId(boardId)

            // Then
            assertEquals(2, response.size) // 최상위 댓글은 2개 (c1, c2)

            // c1 검증
            val r1 = response[0]
            assertEquals(c1.id, r1.id)
            assertEquals(2, r1.replies.size) // c1_1, c1_2

            val r1_1 = r1.replies[0]
            assertEquals(c1_1.id, r1_1.id)
            assertEquals(1, r1_1.replies.size) // c1_1_1

            val r1_2 = r1.replies[1]
            assertEquals(c1_2.id, r1_2.id)
            assertTrue(r1_2.replies.isEmpty())

            val r1_1_1 = r1_1.replies[0]
            assertEquals(c1_1_1.id, r1_1_1.id)
            assertTrue(r1_1_1.replies.isEmpty())

            // c2 검증
            val r2 = response[1]
            assertEquals(c2.id, r2.id)
            assertTrue(r2.replies.isEmpty())

            // 정렬 확인 (insertDts 기준)
            assertTrue(r1.insertDts.isBefore(r2.insertDts))
            assertTrue(r1_1.insertDts.isBefore(r1_2.insertDts))
            assertTrue(r1_1.insertDts.isBefore(r1_1_1.insertDts))

            verify(commentRepository, times(1)).findAllByBoardIdOrderByInsertDtsAsc(boardId)
        }
    }

    @Nested
    @DisplayName("댓글 수정 (updateComment) 테스트")
    inner class UpdateCommentTests {

        @Test
        @DisplayName("성공: 댓글 내용 수정")
        fun `updateComment should update comment content and updateDts`() {
            // Given
            val commentId = 1L
            val originalContent = "원래 댓글 내용"
            val newContent = "수정된 댓글 내용입니다."
            val request = UpdateCommentRequest(content = newContent)
            val originalComment = CommentMst(id = commentId, content = originalContent, author = "작가", boardId = 10L, insertDts = LocalDateTime.now().minusDays(1))

            // Mocking
            `when`(commentRepository.findById(commentId)).thenReturn(java.util.Optional.of(originalComment))
            // save 메소드가 호출되면 전달된 CommentMst 객체를 그대로 반환하도록 모킹
            given(commentRepository.save(any(CommentMst::class.java))).willAnswer { invocation ->
                val commentToSave = invocation.getArgument(0, CommentMst::class.java)
                assertEquals(newContent, commentToSave.content) // 업데이트된 내용이 제대로 적용되었는지 검증
                assertNotNull(commentToSave.updateDts) // updateDts가 설정되었는지 검증
                commentToSave // save 호출 후 반환될 객체
            }

            // When
            val response = commentService.updateComment(commentId, request)

            // Then
            assertNotNull(response)
            assertEquals(commentId, response.id)
            assertEquals(newContent, response.content)
            assertEquals(originalComment.author, response.author) // 저자는 변하지 않음
            assertNotNull(response.updateDts) // updateDts가 설정되었는지 확인
            assertTrue(response.updateDts!!.isAfter(originalComment.insertDts)) // updateDts가 insertDts보다 최신인지 확인

            verify(commentRepository, times(1)).findById(commentId)
            verify(commentRepository, times(1)).save(any(CommentMst::class.java))
        }

        @Test
        @DisplayName("실패: 댓글이 존재하지 않는 경우 IllegalArgumentException 발생")
        fun `updateComment should throw IllegalArgumentException when comment does not exist`() {
            // Given
            val commentId = 999L
            val request = UpdateCommentRequest(content = "수정된 내용")

            // Mocking
            `when`(commentRepository.findById(commentId)).thenReturn(java.util.Optional.empty()) // 댓글이 없는 경우

            // When & Then
            val exception = assertThrows<IllegalArgumentException> {
                commentService.updateComment(commentId, request)
            }
            assertEquals("댓글을 찾을 수 없습니다: $commentId", exception.message)

            verify(commentRepository, times(1)).findById(commentId)
            verify(commentRepository, times(0)).save(any(CommentMst::class.java))
        }
    }

    @Test
    @DisplayName("댓글이 존재하지 않을 경우 IllegalArgumentException을 던진다")
    fun `댓글이_존재하지_않을_경우_IllegalArgumentException을_던진다`() {
        // Given
        val nonExistentCommentId = 999L

        given(commentRepository.findById(nonExistentCommentId)).willReturn(Optional.empty())

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            commentService.deleteComment(nonExistentCommentId)
        }
        assertEquals("댓글을 찾을 수 없습니다: $nonExistentCommentId", exception.message)

        verify(commentRepository, Mockito.times(1)).findById(nonExistentCommentId) // findById가 호출되었는지 검증
        verify(commentRepository, never()).save(any(CommentMst::class.java))
        verify(commentRepository, never()).delete(any(CommentMst::class.java))
    }

    @Test
    @DisplayName("대댓글이 있는 부모 댓글 삭제 시, 논리적 삭제(isDeleted=true)된다")
    fun `대댓글이_있는_부모_댓글_삭제시_논리적_삭제`() {
        // Given
        val parentCommentId = 1L
        val boardId = 10L
        val now = LocalDateTime.now()

        val parentComment = CommentMst(
            id = parentCommentId,
            content = "부모 댓글",
            author = "부모작성자",
            boardId = boardId,
            insertDts = now,
            updateDts = now
        )
        val childComment = CommentMst(
            id = 2L,
            content = "자식 댓글",
            author = "자식작성자",
            boardId = boardId,
            parentCommentId = parentCommentId,
            insertDts = now.plusSeconds(1),
            updateDts = now.plusSeconds(1)
        )

        given(commentRepository.findById(parentCommentId)).willReturn(Optional.of(parentComment))
        given(commentRepository.findAllByParentCommentIdOrderByInsertDtsAsc(parentCommentId))
            .willReturn(listOf(childComment))

        // When
        commentService.deleteComment(parentCommentId)

        // Then
        // save 호출 검증을 위해 ArgumentCaptor 사용
        val commentCaptor = ArgumentCaptor.forClass(CommentMst::class.java)
        verify(commentRepository, times(1)).save(commentCaptor.capture())

        val savedComment = commentCaptor.value
        assertNotNull(savedComment)
        assertEquals(parentCommentId, savedComment.id)
        assertTrue(savedComment.isDeleted) // isDeleted가 true로 변경되었는지 확인

        verify(commentRepository, never()).delete(any(CommentMst::class.java))
        // findById가 제대로 호출되었는지 검증
        verify(commentRepository, times(1)).findById(parentCommentId)
    }
}
