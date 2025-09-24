package com.connect.service.comment

import com.connect.service.board.entity.BoardMst
import com.connect.service.board.repository.BoardRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.* // Mockito-Kotlin 라이브러리 사용 시 간편한 문법을 위해 import
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class CommentServiceTest {

    @Mock // Mock 객체 생성 (Mockito)
    private lateinit var commentMstRepository: CommentMstRepository

    @Mock
    private lateinit var commentDtlRepository: CommentDtlRepository

    @Mock // Mock 객체 생성 (Mockito)
    private lateinit var boardRepository: BoardRepository

    // Mock 객체들을 주입받아 테스트할 서비스 인스턴스 생성 (Mockito)
    @InjectMocks
    private lateinit var commentService: CommentService

    private val boardId = 1L
    private val userId = "yong7317@google.com"
    private val userName = "HHY"
    private lateinit var now: LocalDateTime // 각 테스트 전에 초기화될 현재 시간

    private lateinit var board: BoardMst // 'now'가 초기화된 후 사용되도록 lateinit으로 선언


    @BeforeEach // 각 테스트 시작 전 초기화
    fun setUp() {
        // 각 테스트 시작 전 'now'를 초기화 (나노초 제거하여 비교 용이)
        now = LocalDateTime.now().withNano(0)

        // board 객체를 여기서 초기화하여 'now' 값을 사용할 수 있도록 합니다.
        board = BoardMst(
            id = boardId,
            title = "첫 번째 게시글",
            content = "내용 1",
            category = "자유게시판",
            userId = "testUser@test.com",
            userName = "테스트유저",
            deadlineDts = now.plusDays(1),
            destination = "어딘가",
            maxCapacity = 4,
            currentParticipants = 1,
            commentCount = 0,
            isDeleted = false,
        )
    }

    @Nested
    @DisplayName("댓글 생성 (createComment) 테스트")
    inner class CreateCommentTests {

        @BeforeEach
        fun setupCommonMocksForCreate() {
            `when`(boardRepository.findById(boardId)).thenReturn(Optional.of(board))
        }

        private fun compareLocalDateTimeIgnoringNanos(expected: LocalDateTime, actual: LocalDateTime): Boolean {
            return expected.withNano(0).isEqual(actual.withNano(0))
        }

        @Test
        @DisplayName("성공: 일반 댓글 (CommentMst)이 정상적으로 생성되어야 한다")
        fun `createComment should create CommentMst successfully`() {
            // Given
            val request = CreateCommentRequest(
                content = "새로운 일반 댓글입니다.",
                userId = userId,
                userName = userName,
                parentId = null
            )
            val savedCommentId = 10L

            // 서비스에서 생성된 실제 insertDts, updateDts를 캡처할 변수
            var capturedInsertDts: LocalDateTime? = null
            var capturedUpdateDts: LocalDateTime? = null

            // commentMstRepository.save 모킹: doAnswer().`when`() 패턴 사용
            doAnswer { invocation ->
                val comment = invocation.getArgument(0, CommentMst::class.java)
                // 서비스에서 CommentMst가 생성될 때 설정된 insertDts와 updateDts를 캡처
                capturedInsertDts = comment.insertDts
                capturedUpdateDts = comment.updateDts

                // 저장될 때 ID와 캡처된 시간으로 copy하여 반환 (mock 리턴 값)
                comment.copy(
                    id = savedCommentId,
                )
            }.`when`(commentMstRepository).save(any(CommentMst::class.java))

            // When
            val response = commentService.createComment(boardId, request)

            // Then
            assertNotNull(response)
            assertEquals(savedCommentId, response.id)
            assertEquals(request.content, response.content)
            assertEquals(request.userId, response.userId)
            assertEquals(request.userName, response.userName)
            assertNull(response.parentId)

            // 캡처된 실제 시간과 DTO 응답 시간을 나노초 무시하고 비교
            assertNotNull(capturedInsertDts)
            assertNotNull(capturedUpdateDts)
            assertTrue(compareLocalDateTimeIgnoringNanos(capturedInsertDts!!, response.insertDts), "insertDts should match (ignoring nanoseconds)")
            assertTrue(compareLocalDateTimeIgnoringNanos(capturedUpdateDts!!, response.updateDts), "updateDts should match (ignoring nanoseconds)")


            // Verify
            verify(boardRepository, times(1)).findById(boardId)
            verify(commentMstRepository, times(1)).save(any(CommentMst::class.java))
            verify(commentMstRepository, never()).findById(any(Long::class.java))
            verify(commentDtlRepository, never()).save(any(CommentDtl::class.java))
        }

        @Test
        @DisplayName("성공: 대댓글 (CommentDtl)이 정상적으로 생성되어야 한다")
        fun `createComment should create CommentDtl successfully`() {
            // Given
            val parentCommentId = 100L
            val parentCommentMst = CommentMst(
                id = parentCommentId,
                userId = "parent@example.com",
                userName = "부모댓글러",
                content = "나는 부모 댓글이다.",
                postId = boardId,
            )

            val request = CreateCommentRequest(
                content = "새로운 대댓글입니다.",
                userId = userId,
                userName = userName,
                parentId = parentCommentId
            )
            val savedReplyId = 200L

            // 서비스에서 생성된 실제 insertDts, updateDts를 캡처할 변수 (대댓글용)
            var capturedReplyInsertDts: LocalDateTime? = null
            var capturedReplyUpdateDts: LocalDateTime? = null


            // commentMstRepository.findById (-> 서비스 내 findByIdOrNull) 모킹
            `when`(commentMstRepository.findById(parentCommentId)).thenReturn(Optional.of(parentCommentMst))


            // commentDtlRepository.save 모킹: doAnswer().`when`() 패턴 사용
            doAnswer { invocation ->
                val reply = invocation.getArgument(0, CommentDtl::class.java)
                // 서비스에서 CommentDtl이 생성될 때 설정된 insertDts와 updateDts를 캡처
                capturedReplyInsertDts = reply.insertDts
                capturedReplyUpdateDts = reply.updateDts

                // 저장될 때 ID와 캡처된 시간으로 copy하여 반환 (mock 리턴 값)
                reply.copy(
                    id = savedReplyId,
                )
            }.`when`(commentDtlRepository).save(any(CommentDtl::class.java))

            // 부모 CommentMst의 replies 리스트 업데이트 후 저장되는 부분을 `doReturn().when()`으로 모킹
            // `parentCommentMst`라는 특정 객체 인스턴스를 인자로 받으므로 `doReturn`이 더 안전함.
            doReturn(parentCommentMst).`when`(commentMstRepository).save(parentCommentMst)


            // When
            val response = commentService.createComment(boardId, request)

            // Then
            assertNotNull(response)
            assertEquals(savedReplyId, response.id)
            assertEquals(request.content, response.content)
            assertEquals(request.userId, response.userId)
            assertEquals(request.userName, response.userName)
            assertEquals(parentCommentId, response.parentId)

            // 캡처된 실제 시간과 DTO 응답 시간을 나노초 무시하고 비교
            assertNotNull(capturedReplyInsertDts)
            assertNotNull(capturedReplyUpdateDts)
            assertTrue(compareLocalDateTimeIgnoringNanos(capturedReplyInsertDts!!, response.insertDts), "insertDts should match (ignoring nanoseconds)")
            assertTrue(compareLocalDateTimeIgnoringNanos(capturedReplyUpdateDts!!, response.updateDts), "updateDts should match (ignoring nanoseconds)")

            // Verify
            verify(boardRepository, times(1)).findById(boardId)
            // 서비스 코드에서 findByIdOrNull이 내부적으로 findById를 호출하므로 findById를 검증합니다.
            verify(commentMstRepository, times(1)).findById(parentCommentId)
            verify(commentDtlRepository, times(1)).save(any(CommentDtl::class.java))
            // 부모 CommentMst의 replies 리스트 변경 후 저장하는 호출 검증
            verify(commentMstRepository, times(1)).save(parentCommentMst)
        }

        @Test
        @DisplayName("실패: 게시글이 존재하지 않으면 IllegalArgumentException이 발생해야 한다")
        fun `createComment should throw exception if board does not exist`() {
            val invalidBoardId = 999L
            val request = CreateCommentRequest(content = "내용", userId = userId, userName = userName, parentId = null)

            `when`(boardRepository.findById(invalidBoardId)).thenReturn(Optional.empty())

            val exception = assertThrows<IllegalArgumentException> {
                commentService.createComment(invalidBoardId, request)
            }
            assertEquals("게시글을 찾을 수 없습니다: $invalidBoardId", exception.message)

            verify(boardRepository, times(1)).findById(invalidBoardId)
            verify(commentMstRepository, never()).save(any(CommentMst::class.java))
            verify(commentDtlRepository, never()).save(any(CommentDtl::class.java))
        }

        @Test
        @DisplayName("실패: 부모 댓글이 존재하지 않으면 IllegalArgumentException이 발생해야 한다")
        fun `createComment should throw exception if parent comment does not exist`() {
            val invalidParentId = 999L
            val request = CreateCommentRequest(
                content = "내용",
                userId = userId,
                userName = userName,
                parentId = invalidParentId
            )

            // 부모 댓글은 존재하지 않음 (이 부분만 Mocking)
            `when`(commentMstRepository.findById(invalidParentId)).thenReturn(Optional.empty())

            val exception = assertThrows<IllegalArgumentException> {
                commentService.createComment(boardId, request)
            }
            assertEquals("부모 댓글을 찾을 수 없습니다: $invalidParentId", exception.message)

            verify(boardRepository, times(1)).findById(boardId)
            verify(commentMstRepository, times(1)).findById(invalidParentId)
            verify(commentMstRepository, never()).save(any(CommentMst::class.java))
            verify(commentDtlRepository, never()).save(any(CommentDtl::class.java))
        }
    }
}
