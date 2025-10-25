package com.connect.service.comment

import com.connect.service.board.entity.BoardMst
import com.connect.service.board.repository.BoardRepository
import com.connect.service.comment.domain.ReplyEntity
import com.connect.service.comment.repository.ReplyRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.* // Mockito-Kotlin 라이브러리 사용 시 간편한 문법을 위해 import
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

    @Mock // ReplyRepository 인터페이스를 Mocking하여 가짜 객체 생성
    private lateinit var replyRepository: ReplyRepository

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
    }

    @Test
    fun `댓글 삭제_성공_정상적인_댓글이_존재할_때`() {
        // Given (테스트를 위한 준비)
        val boardId = 1L
        val replyId = 100
        // 여기! 모든 필드에 값을 전달하도록 수정했어.
        val mockReply = ReplyEntity(
            id = replyId,
            postId = boardId,
            userId = "testUser",
            userName = "테스트 유저",
            title = "테스트 제목", // Nullable 필드는 Null이거나 값이 있을 수 있어
            content = "테스트 댓글 내용",
            insertDts = LocalDateTime.now(), // 현재 시간으로 설정
            parentReplyId = null // 대댓글이 아니므로 Null
        )

        // replyRepository.findByPostIdAndId 호출 시, mockReply를 Optional.of(mockReply)로 반환하도록 설정
        `when`(replyRepository.findByPostIdAndId(boardId, replyId)).thenReturn(Optional.of(mockReply))

        // When (실제 메소드 호출)
        commentService.deleteComment(boardId, replyId)

        // Then (결과 검증)
        // 1. findByPostIdAndId 메소드가 정확히 boardId와 replyId로 한 번 호출되었는지 검증
        verify(replyRepository, times(1)).findByPostIdAndId(boardId, replyId)
        // 2. delete 메소드가 mockReply 객체로 한 번 호출되었는지 검증
        verify(replyRepository, times(1)).delete(mockReply)
        // 3. 다른 Mocked 메소드는 호출되지 않았는지 검증
        verifyNoMoreInteractions(replyRepository)
    }
}
