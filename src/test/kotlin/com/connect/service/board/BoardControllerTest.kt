package com.connect.service.board

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
@WebMvcTest(BoardController::class)
@ContextConfiguration(classes = [com.connect.service.ConnectApplication::class])
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
        val boardList = listOf(
            BoardMst(id = 1L, title = "첫 번째 게시글", content = "내용 1", author = "김개발"),
            BoardMst(id = 2L, title = "두 번째 게시글", content = "내용 2", author = "박테스트")
        )
        // when(boardService.getAllBoards()).thenReturn(boardList) // 이렇게 쓰는 것도 좋아!
        given(boardService.getAllBoards()).willReturn(boardList)

        // When & Then: GET 요청을 보내고 결과 검증
        mockMvc.perform(get("/api/boards")
            .contentType(MediaType.APPLICATION_JSON)) // 요청 타입이 JSON이라고 명시
            .andExpect(status().isOk) // HTTP 200 OK인지 확인
            .andExpect(jsonPath("$[0].title").value("첫 번째 게시글")) // 첫 번째 게시글의 제목 검증
            .andExpect(jsonPath("$[1].author").value("박테스트")) // 두 번째 게시글의 작성자 검증
            .andExpect(jsonPath("$.length()").value(2)) // 리스트의 길이가 2인지 검증
    }

    @Test
    fun `POST_api_boards_요청시_새로운_게시글을_생성하고_반환해야_한다`() {
        // Given: 새로운 게시글 생성 요청 데이터와 Service가 반환할 가상의 결과 데이터 생성
        val request = CreateBoardRequest(title = "새 게시글 제목", content = "새 게시글 내용", author = "새로운 작성자")
        val createdBoard = BoardMst(id = 3L, title = "새 게시글 제목", content = "새 게시글 내용", author = "새로운 작성자")

        // given(boardService.createBoard(anyString(), anyString(), anyString())).willReturn(createdBoard) // 이렇게 좀 더 일반화해서 쓸 수도 있어
        // anyString()을 쓰려면 Mockito.anyString()을 static import 해줘야 해!
        // 여기서는 정확한 값을 써주는게 더 테스트 의도를 명확하게 보여줘 :)
        given(boardService.createBoard(request.title, request.content, request.author)).willReturn(createdBoard)

        // When & Then: POST 요청을 보내고 결과 검증
        mockMvc.perform(post("/api/boards")
            .contentType(MediaType.APPLICATION_JSON) // 요청 타입이 JSON이라고 명시
            .content(objectMapper.writeValueAsString(request))) // 요청 본문을 JSON 문자열로 변환하여 전송
            .andExpect(status().isOk) // HTTP 200 OK인지 확인
            .andExpect(jsonPath("$.id").value(3L)) // 생성된 게시글의 ID 검증
            .andExpect(jsonPath("$.title").value("새 게시글 제목")) // 제목 검증
            .andExpect(jsonPath("$.content").value("새 게시글 내용")) // 내용 검증
            .andExpect(jsonPath("$.author").value("새로운 작성자")) // 작성자 검증
    }
}

// 테스트를 위해 필요한 DTO 클래스도 정의해줘!
// 실제 프로젝트에선 별도의 파일에 정의되어 있을 거야 :)
data class BoardMst(
    val id: Long,
    val title: String,
    val content: String,
    val author: String
)

data class CreateBoardRequest(
    val title: String,
    val content: String,
    val author: String
)
