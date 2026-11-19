package com.connect.service.board

import com.connect.service.board.entity.BoardMst
import com.connect.service.board.entity.BoardParticipant
import com.connect.service.board.repository.BoardParticipantRepository
import com.connect.service.board.repository.BoardRepository
import com.connect.service.board.service.BoardParticipantService
import com.connect.service.user.domain.Users
import com.connect.service.user.repository.UserRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BoardParticipantServiceTest {

    @Mock
    private lateinit var boardRepository: BoardRepository

    @Mock
    private lateinit var participantRepository: BoardParticipantRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @InjectMocks
    private lateinit var service: BoardParticipantService

    private fun board(
        current: Int = 1,
        max: Int = 4,
        deadline: LocalDateTime = LocalDateTime.now().plusDays(1),
        author: String = "author",
        deleted: Boolean = false,
    ) = BoardMst(
        id = 10L,
        title = "강변역",
        content = "같이 가요",
        category = "같이타",
        userId = author,
        userName = "작성자",
        deadlineDts = deadline,
        destination = "강변역",
        maxCapacity = max,
        currentParticipants = current,
        isDeleted = deleted,
    )

    private fun givenUser(userId: String) {
        val user = mock<Users>()
        whenever(user.userId).thenReturn(userId)
        whenever(user.name).thenReturn("참여자")
        whenever(userRepository.findByUserId(userId)).thenReturn(user)
    }

    private fun assertConflict(block: () -> Unit) {
        val ex = assertThrows<ResponseStatusException> { block() }
        assertEquals(HttpStatus.CONFLICT, ex.statusCode)
    }

    @Test
    @DisplayName("참여하면 참여자 행이 저장되고 인원이 1 늘며 joined=true 로 응답한다")
    fun `참여_성공`() {
        val b = board(current = 1)
        givenUser("joiner")
        whenever(boardRepository.findByIdForUpdate(10L)).thenReturn(b)
        whenever(participantRepository.existsByBoardIdAndUserId(10L, "joiner")).thenReturn(false)
        whenever(boardRepository.save(any<BoardMst>())).thenAnswer { it.arguments[0] }

        val res = service.join(10L, "joiner")

        assertEquals(2, res.currentParticipants)
        assertTrue(res.joined)
        verify(participantRepository).save(any<BoardParticipant>())
    }

    @Test
    @DisplayName("작성자, 이미 참여한 사람, 마감일 지난 글, 정원 찬 글은 409")
    fun `참여_거절`() {
        givenUser("author")
        whenever(boardRepository.findByIdForUpdate(10L)).thenReturn(board())
        assertConflict { service.join(10L, "author") }

        givenUser("joiner")
        whenever(participantRepository.existsByBoardIdAndUserId(10L, "joiner")).thenReturn(true)
        assertConflict { service.join(10L, "joiner") }

        whenever(participantRepository.existsByBoardIdAndUserId(10L, "joiner")).thenReturn(false)
        whenever(boardRepository.findByIdForUpdate(10L))
            .thenReturn(board(deadline = LocalDateTime.now().minusMinutes(1)))
        assertConflict { service.join(10L, "joiner") }

        whenever(boardRepository.findByIdForUpdate(10L)).thenReturn(board(current = 4, max = 4))
        assertConflict { service.join(10L, "joiner") }

        verify(participantRepository, never()).save(any<BoardParticipant>())
    }

    @Test
    @DisplayName("삭제됐거나 없는 글은 404")
    fun `없는_글`() {
        givenUser("joiner")
        whenever(boardRepository.findByIdForUpdate(10L)).thenReturn(board(deleted = true))
        val ex = assertThrows<ResponseStatusException> { service.join(10L, "joiner") }
        assertEquals(HttpStatus.NOT_FOUND, ex.statusCode)
    }

    @Test
    @DisplayName("참여 취소하면 행이 지워지고 인원이 1 줄며 joined=false, 참여 안 했으면 409")
    fun `참여_취소`() {
        val b = board(current = 3)
        val p = BoardParticipant(id = 1L, boardId = 10L, userId = "joiner", userName = "참여자")
        whenever(boardRepository.findByIdForUpdate(10L)).thenReturn(b)
        whenever(participantRepository.findByBoardIdAndUserId(10L, "joiner")).thenReturn(p)
        whenever(boardRepository.save(any<BoardMst>())).thenAnswer { it.arguments[0] }

        val res = service.leave(10L, "joiner")

        assertEquals(2, res.currentParticipants)
        assertFalse(res.joined)
        verify(participantRepository).delete(p)

        whenever(participantRepository.findByBoardIdAndUserId(10L, "other")).thenReturn(null)
        assertConflict { service.leave(10L, "other") }
    }
}
