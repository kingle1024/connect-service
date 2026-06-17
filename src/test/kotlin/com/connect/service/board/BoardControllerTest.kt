package com.connect.service.board

import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import com.connect.service.board.controller.BoardController
import com.connect.service.board.dto.BoardCreateRequest
import com.connect.service.board.dto.BoardResponseDto
import com.connect.service.board.dto.PaginatedBoardResponse
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import com.connect.service.board.dto.UpdateBoardRequest
import com.connect.service.board.entity.BoardMst
import com.connect.service.board.service.BoardService
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.springframework.data.domain.Pageable
import org.springframework.security.test.context.support.WithMockUser
import java.util.Optional

@WebMvcTest(BoardController::class)
@WithMockUser
class BoardControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var boardService: BoardService
    inline fun <reified T> mockitoAny(): T {
        any(T::class.java)
        return uninitialized()
    }

    // 초기화되지 않은 더미 값을 반환하는 헬퍼 함수
    // Mockito의 `any()` 매처는 실제로 객체를 반환하는 것이 아니라 내부적으로 매처를 등록만 하기 때문에
    // Kotlin 컴파일러가 요구하는 non-null 반환 값을 제공하기 위한 트릭입니다.
    fun <T> uninitialized(): T = null as T

    @Test
    fun `getBoardDetail - 게시글이 존재하지 않을 경우 404 Not Found 반환`() {
        // given: 테스트를 위한 mock 데이터 설정
        val boardId = 999L // 존재하지 않는 ID

        // when: boardService.getBoardDetail(boardId) 호출 시 null 반환하도록 Mocking
        `when`(boardService.getBoardDetail(boardId)).thenReturn(null)

        // then: MockMvc를 사용하여 HTTP GET 요청을 보내고 결과를 검증
        mockMvc.perform(
            get("/api/boards/{id}", boardId) // 컨트롤러의 @GetMapping 경로에 맞춰 호출 (예: /boards/999)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound) // HTTP 상태 코드가 404 Not Found인지 검증

        // verify: boardService.getBoardDetail(boardId) 메서드가 1번 호출되었는지 검증
        verify(boardService, times(1)).getBoardDetail(boardId)
    }

    @Test
    fun `getBoardDetail - 게시글이 존재할 경우 200 OK와 게시글 반환`() {
        // given: 테스트를 위한 mock 데이터 설정
        val boardId = 1L
        val expectedBoard = BoardResponseDto(
            id = boardId,
            title = "새로운 제목",
            content = "더욱 풍성한 내용입니다.",
            category = "운동",
            commentCount = 3,
            userId = "100",
            userName = "김테스트",
            insertDts = LocalDateTime.now(),
            deadlineDts = LocalDateTime.of(2026, 1, 31, 23, 59),
            destination = "한강공원",
            maxCapacity = 5,
            currentParticipants = 2,
            verified = true
        )

        // when: boardService.getBoardDetail(boardId) 호출 시 DTO 반환하도록 Mocking
        `when`(boardService.getBoardDetail(boardId)).thenReturn(expectedBoard)

        // then: MockMvc를 사용하여 HTTP GET 요청을 보내고 결과를 검증
        mockMvc.perform(
            get("/api/boards/{id}", boardId) // 컨트롤러의 @GetMapping 경로에 맞춰 호출 (예: /boards/1)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk) // HTTP 상태 코드가 200 OK인지 검증
            .andExpect(jsonPath("$.id").value(expectedBoard.id)) // 응답 본문의 id 필드 검증
            .andExpect(jsonPath("$.title").value(expectedBoard.title)) // 응답 본문의 title 필드 검증
            .andExpect(jsonPath("$.content").value(expectedBoard.content)) // 응답 본문의 content 필드 검증
            .andExpect(jsonPath("$.verified").value(true)) // 작성자 인증 여부 검증

        // verify: boardService.getBoardDetail(boardId) 메서드가 1번 호출되었는지 검증
        verify(boardService, times(1)).getBoardDetail(boardId)
    }

    @Test
    fun `getBoards 호출 시 페이지네이션된 게시글 목록과 다음 페이지 토큰을 반환해야 한다`() {
        // given
        val now = LocalDateTime.now()
        val boardDto1 = BoardResponseDto(
            id = 1L, title = "첫 번째 게시글", content = "내용1", category = "자유", commentCount = 5,
            userId = "user01", userName = "김철수", insertDts = now.minusDays(1),
            deadlineDts = now.plusDays(1), destination = "서울", maxCapacity = 10, currentParticipants = 3
        )
        val boardDto2 = BoardResponseDto(
            id = 2L, title = "두 번째 게시글", content = "내용2", category = "질문", commentCount = 12,
            userId = "user02", userName = "이영희", insertDts = now,
            deadlineDts = now.plusDays(2), destination = "부산", maxCapacity = 5, currentParticipants = 2
        )
        val boardList = listOf(boardDto2, boardDto1)

        val paginatedResponseWithNextPage = PaginatedBoardResponse(
            nextPageToken = 2,
            posts = boardList
        )
        `when`(boardService.getAllBoards(mockitoAny<Pageable>())).thenReturn(paginatedResponseWithNextPage)

        // when
        mockMvc.perform(get("/api/boards")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.posts[0].id").value(boardDto2.id))
            .andExpect(jsonPath("$.posts[0].title").value(boardDto2.title))
            .andExpect(jsonPath("$.posts[0].commentCount").value(boardDto2.commentCount))
            .andExpect(jsonPath("$.posts[0].currentParticipants").value(boardDto2.currentParticipants))
            .andExpect(jsonPath("$.nextPageToken").value(2))

        // verify
        verify(boardService).getAllBoards(mockitoAny<Pageable>())
    }

    @Test
    fun `getBoards 호출 시 더 이상 게시글이 없으면 nextPageToken이 null이어야 한다`() {
        // given
        val now = LocalDateTime.now()
        val boardDto1 = BoardResponseDto(
            id = 3L, title = "마지막 페이지 게시글", content = "내용3", category = "공지", commentCount = 1,
            userId = "admin", userName = "관리자", insertDts = now,
            deadlineDts = now.plusDays(5), destination = "본사", maxCapacity = 100, currentParticipants = 10
        )
        val boardList = listOf(boardDto1)

        val paginatedResponseNoNextPage = PaginatedBoardResponse(
            nextPageToken = null,
            posts = boardList
        )
        // Mocking 시에도 `mockitoAny()` 사용
        `when`(boardService.getAllBoards(mockitoAny<Pageable>()))
            .thenReturn(paginatedResponseNoNextPage)

        // when
        mockMvc.perform(get("/api/boards")
            .param("page", "1") // 1페이지를 요청하여 마지막 페이지 시나리오 테스트
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.posts[0].id").value(boardDto1.id))
            .andExpect(jsonPath("$.nextPageToken").isEmpty)

        // verify
        verify(boardService).getAllBoards(mockitoAny<Pageable>())
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
            .with(user("initialUserId"))
            .with(csrf())
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
       // Given (테스트를 위한 사전 준비):
       val boardId = 1L
       val initialUserId = "testUser@example.com" // 가상의 로그인 사용자 ID
       val initialUserName = "테스트사용자" // BoardMst에 포함될 사용자 이름
       val updatedTitle = "수정된 제목입니다."
       val updatedContent = "새로운 내용으로 변경했습니다."
       val initialCategory = "스터디"
       val updatedDeadline = LocalDateTime.now().plusDays(2).withNano(0) // 정밀도 제거

       // 서비스가 반환할 BoardMst 객체 (업데이트된 상태)
       val updatedBoard = BoardMst(
           id = boardId,
           title = updatedTitle,
           content = updatedContent,
           category = initialCategory,
           userId = initialUserId, // 요청 보낸 사용자 ID와 동일하게 설정
           userName = initialUserName,
           deadlineDts = updatedDeadline,
           destination = "업데이트된 목적지",
           maxCapacity = 5,
           currentParticipants = 3,
           commentCount = 10,
           isDeleted = false
       )

       // Mocking: boardService.updateBoard() 호출 시 updatedBoard를 반환하도록 설정
       // 컨트롤러의 updateBoard 메서드는 request.id, request.title, request.content, request.author를 서비스로 넘기므로,
       // Mockito의 given()도 이에 맞춰야 한다.
       given(boardService.updateBoard(
           eq(boardId), // request.id (pathId와 같음)
           eq(updatedTitle), // request.title
           eq(updatedContent), // request.content
           eq(initialUserId) // request.author
       )).willReturn(updatedBoard)

       // 요청 본문으로 보낼 데이터
       val updateRequest = UpdateBoardRequest(
           id = boardId, // path ID와 동일하게 설정하여 IllegalArgumentException 회피
           title = updatedTitle,
           content = updatedContent,
           author = initialUserId // request body에도 author 정보 포함
       )

       // When (PUT 요청 실행):
       mockMvc.perform(put("/api/boards/{id}", boardId) // 컨트롤러 @PutMapping("/{id}") 경로와 일치
           .with(user(initialUserId)) // 로그인한 사용자 정보 (initialUserId를 `author`로 사용)
           .with(csrf())               // CSRF 토큰 추가 (PUT 요청이므로 필수)
           .contentType(MediaType.APPLICATION_JSON)
           .content(objectMapper.writeValueAsString(updateRequest))) // UpdateBoardRequest를 JSON으로 변환하여 요청 본문에 넣음
           .andExpect(status().isOk) // HTTP 200 OK 응답 검증
           .andExpect(jsonPath("$.id").value(boardId)) // JSON 응답의 id 필드 검증
           .andExpect(jsonPath("$.title").value(updatedTitle)) // JSON 응답의 title 필드 검증
           .andExpect(jsonPath("$.content").value(updatedContent)) // JSON 응답의 content 필드 검증
           .andExpect(jsonPath("$.userId").value(initialUserId)) // userId도 검증
           .andExpect(jsonPath("$.userName").value(initialUserName)) // userName도 검증
           .andExpect(jsonPath("$.deadlineDts").value(updatedDeadline.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))


       // Then (서비스 호출 검증):
       // boardService.updateBoard()가 정확히 한 번, 특정 인자들로 호출되었는지 검증
       verify(boardService, times(1)).updateBoard(
           eq(boardId),
           eq(updatedTitle),
           eq(updatedContent),
           eq(initialUserId)
       )
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
            .with(user("initialUserId")) // 로그인한 사용자 정보 (initialUserId를 `author`로 사용)
            .with(csrf())               // CSRF 토큰 추가 (PUT 요청이므로 필수)
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
            .with(user("initialUserId")) // 로그인한 사용자 정보 (initialUserId를 `author`로 사용)
            .with(csrf())               // CSRF 토큰 추가 (PUT 요청이므로 필수)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isBadRequest) // 400 Bad Request 반환 검증
            .andExpect(content().string("잘못된 요청입니다: ID in path ($pathId) must match ID in request body ($bodyId)"))

        verify(boardService, never()).updateBoard(anyLong(), anyString(), anyString(), anyOrNull())
    }
}
