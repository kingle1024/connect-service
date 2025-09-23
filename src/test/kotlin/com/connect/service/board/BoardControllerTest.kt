package com.connect.service.board

import com.connect.service.ConnectApplication
import com.connect.service.board.controller.BoardController
import com.connect.service.board.dto.BoardCreateRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import com.connect.service.board.dto.UpdateBoardRequest
import com.connect.service.board.entity.BoardMst
import com.connect.service.board.service.BoardService
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.mockito.kotlin.anyOrNull
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@WebMvcTest(BoardController::class)
@ContextConfiguration(classes = [ConnectApplication::class])
class BoardControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    // BoardService는 실제 빈을 사용하는 대신 목(Mock) 객체로 주입해서 컨트롤러만 테스트할 수 있게 해줘
    @MockitoBean
    private lateinit var boardService: BoardService

    @Test
    fun `GET_api_boards_요청시_모든_게시글을_반환해야_한다`() {
        // Given: BoardService가 호출되면 반환할 가상의 데이터 생성
        val now = LocalDateTime.now()
        val boardList = listOf(
            BoardMst(
                id = 1L,
                title = "첫 번째 게시글",
                content = "내용 1",
                category = "자유게시판", // 새로운 필드 추가
                userId = "kim.dev@example.com", // 새로운 필드 추가
                userName = "김개발", // 새로운 필드 추가 (이전 author 역할)
                deadlineDts = now.plusDays(1), // 마감일 (적절한 시간 설정)
                destination = "강촌역",
                maxCapacity = 4, // 새로운 필드 추가
                currentParticipants = 1, // 새로운 필드 추가
                commentCount = 0,
                isDeleted = false
            ),
            BoardMst(
                id = 2L,
                title = "두 번째 게시글",
                content = "내용 2",
                category = "QnA", // 새로운 필드 추가
                userId = "park.test@example.com", // 새로운 필드 추가
                userName = "박테스트", // 새로운 필드 추가 (이전 author 역할)
                deadlineDts = now.plusDays(2), // 마감일 (적절한 시간 설정)
                destination = "석사동",
                maxCapacity = 3, // 새로운 필드 추가
                currentParticipants = 2, // 새로운 필드 추가
                commentCount = 5,
                isDeleted = false
            )
        )
        // when(boardService.getAllBoards()).thenReturn(boardList) // 이렇게 쓰는 것도 좋아!
        given(boardService.getAllBoards()).willReturn(boardList)

        // When & Then: GET 요청을 보내고 결과 검증
        mockMvc.perform(get("/api/boards")
            .contentType(MediaType.APPLICATION_JSON)) // 요청 타입이 JSON이라고 명시
            .andExpect(status().isOk) // HTTP 200 OK인지 확인
            .andExpect(jsonPath("$[0].title").value("첫 번째 게시글")) // 첫 번째 게시글의 제목 검증
            .andExpect(jsonPath("$[1].userId").value("park.test@example.com")) // 두 번째 게시글의 작성자 검증
            .andExpect(jsonPath("$.length()").value(2)) // 리스트의 길이가 2인지 검증
    }

    @Test
    fun `POST_api_boards_요청시_새로운_게시글을_생성하고_반환해야_한다`() {
        // Given: 새로운 게시글 생성 요청 데이터와 Service가 반환할 가상의 결과 데이터 생성
        val now = LocalDateTime.now().withNano(0)
        val tomorrow = now.plusDays(1).withNano(0)

        val request = BoardCreateRequest(
            title = "새 게시글 제목",
            content = "새 게시글 내용",
            category = "일상",
            userId = "new.user@example.com",
            userName = "새로운 작성자",
            deadlineDts = tomorrow,
            destination = "남춘천역",
            maxCapacity = 5,
            currentParticipants = 1
        )

        val createdBoard = BoardMst(
            id = 3L,
            title = request.title,
            content = request.content,
            category = request.category,
            userId = request.userId,
            userName = request.userName,
            deadlineDts = request.deadlineDts,
            destination = request.destination,
            maxCapacity = request.maxCapacity,
            currentParticipants = request.currentParticipants,
            commentCount = 0, // 기본값
            isDeleted = false // 기본값
        )

        given(boardService.createBoard(request)).willReturn(createdBoard)

        // When & Then: POST 요청을 보내고 결과 검증
        mockMvc.perform(post("/api/boards")
            .contentType(MediaType.APPLICATION_JSON) // 요청 타입이 JSON이라고 명시
            .content(objectMapper.writeValueAsString(request))) // 요청 본문을 JSON 문자열로 변환하여 전송
            .andExpect(status().isOk) // HTTP 200 OK인지 확인
            .andExpect(jsonPath("$.id").value(3L)) // 생성된 게시글의 ID 검증
            .andExpect(jsonPath("$.title").value("새 게시글 제목")) // 제목 검증
            .andExpect(jsonPath("$.content").value("새 게시글 내용")) // 내용 검증
            .andExpect(jsonPath("$.userId").value("new.user@example.com")) // 작성자 검증
    }

    @Test
    fun `게시글을 성공적으로 수정해야 한다`() {
        val boardId = 1L // 가상의 ID
        val updatedTitle = "수정된 제목입니다."
        val updatedContent = "새로운 내용으로 변경했습니다."
        val initialCategory = "스터디"
        val initialUserId = "original.user@example.com"
        val initialUserName = "원래작성자" // 이전 'author' 역할
        val initialDeadline = LocalDateTime.now().plusHours(1).withNano(0)
        val updatedDeadline = LocalDateTime.now().plusDays(2).withNano(0) // 수정된 마감일
        val initialDestination = "강촌역"
        val updatedDestination = "남춘천역" // 수정된 목적지
        val initialMaxCapacity = 4
        val updatedMaxCapacity = 5 // 수정된 최대 인원
        val initialCurrentParticipants = 2
        val updatedCurrentParticipants = 3 // 수정된 현재 인원

        // Mock 서비스가 반환할 결과 객체 생성
        val updatedBoard = BoardMst(
            id = boardId,
            title = updatedTitle,
            content = updatedContent,
            category = initialCategory, // 카테고리는 변경되지 않았다고 가정
            userId = initialUserId, // 작성자 ID는 변경되지 않는다고 가정
            userName = initialUserName, // 작성자 이름도 변경되지 않는다고 가정
            deadlineDts = updatedDeadline, // 마감일은 변경될 수 있음
            destination = updatedDestination, // 목적지 변경
            maxCapacity = updatedMaxCapacity, // 최대 모집 인원 변경
            currentParticipants = updatedCurrentParticipants, // 현재 참여 인원 변경
            commentCount = 10, // 기존 댓글 수는 그대로
            isDeleted = false // 삭제 상태도 그대로
        )


        // Mockito BDD 스타일로 행동 정의: boardService.updateBoard가 특정 인자로 호출되면 updatedBoard를 반환해라
        given(boardService.updateBoard(boardId, updatedTitle, updatedContent, null))
            .willReturn(updatedBoard)

        // 수정 요청 데이터 준비
        val updateRequest = UpdateBoardRequest(
            id = boardId,
            title = updatedTitle,
            content = updatedContent
        )

        // PUT 요청 실행 및 응답 검증
        mockMvc.perform(put("/api/boards/{id}", boardId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk) // HTTP 200 OK
            .andExpect(jsonPath("$.id").value(boardId))
            .andExpect(jsonPath("$.title").value(updatedTitle))
            .andExpect(jsonPath("$.content").value(updatedContent))
            .andExpect(jsonPath("$.userName").value(initialUserName))
            .andExpect(jsonPath("$.category").value(initialCategory))
            .andExpect(jsonPath("$.userId").value(initialUserId))
            .andExpect(jsonPath("$.destination").value(updatedDestination))
            .andExpect(jsonPath("$.maxCapacity").value(updatedMaxCapacity))
            .andExpect(jsonPath("$.currentParticipants").value(updatedCurrentParticipants))
            .andExpect(jsonPath("$.deadlineDts").value(updatedDeadline.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))

        // (선택적) Mockito verify를 통해 boardService의 updateBoard 메서드가 정확히 한 번 호출되었는지 검증
        verify(boardService, times(1)).updateBoard(boardId, updatedTitle, updatedContent, null)
    }

    @Test
    fun `존재하지 않는 게시글을 수정 시도 시 404 Not Found를 반환해야 한다`() {
        val nonExistentId = 999L // 존재하지 않는 가상의 ID
        val updatedTitle = "새 제목"
        val updatedContent = "새 내용"
        val updateRequest = UpdateBoardRequest(
            id = nonExistentId,
            title = updatedTitle,
            content = updatedContent
        )

        // Mocking: boardService.updateBoard()가 호출되면 NoSuchElementException을 던지도록 설정
        given(boardService.updateBoard(nonExistentId, updatedTitle, updatedContent, null))
            .willThrow(NoSuchElementException("Board with ID $nonExistentId not found"))

        mockMvc.perform(put("/api/boards/{id}", nonExistentId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isNotFound) // 404 Not Found 반환 검증
            .andExpect(content().string("요청하신 리소스를 찾을 수 없습니다: Board with ID $nonExistentId not found")) // 에러 메시지 검증 (GlobalExceptionHandler 메시지)

        verify(boardService, times(1)).updateBoard(nonExistentId, updatedTitle, updatedContent, null)
    }

    @Test
    fun `경로 ID와 요청 본문 ID가 일치하지 않으면 400 Bad Request를 반환해야 한다`() {
        val pathId = 1L
        val bodyId = 2L // 경로 ID와 다른 ID

        val updateRequest = UpdateBoardRequest(
            id = bodyId, // 본문 ID가 경로 ID와 다름
            title = "수정된 제목",
            content = "수정된 내용"
        )

        // 이 경우, 컨트롤러의 자체 로직(id != request.id 체크)이 먼저 실행되므로
        // boardService.updateBoard는 호출되지 않는다. 따라서 Mocking 불필요.
        // 다만, verify를 통해 호출되지 않았음을 명시적으로 확인할 수 있다.

        mockMvc.perform(put("/api/boards/{id}", pathId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isBadRequest) // 400 Bad Request 반환 검증
            .andExpect(content().string("잘못된 요청입니다: ID in path ($pathId) must match ID in request body ($bodyId)"))

        verify(boardService, never()).updateBoard(anyLong(), anyString(), anyString(), anyOrNull())
    }
}
