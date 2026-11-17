package com.connect.service.comment

import com.connect.service.board.repository.BoardRepository
import com.connect.service.comment.domain.ReplyDto
import com.connect.service.comment.domain.ReplyEntity
import com.connect.service.comment.repository.ReplyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.IllegalArgumentException
import com.connect.service.comment.domain.toReplyDto
import com.connect.service.user.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

@Service
@Transactional(readOnly = true)
class CommentService(
    private val replyRepository: ReplyRepository,
    private val boardRepository: BoardRepository,
    private val userRepository: UserRepository
) {

    fun getCommentsByBoardId(boardId: Long): List<ReplyDto> {
        val allReplies = replyRepository.findAllByPostIdOrderByInsertDtsAsc(boardId)
        val replyMap = allReplies
            .associateBy { it.id }
            .mapValues { (_, entity: ReplyEntity) -> entity.toReplyDto(boardId) }
            .toMutableMap()

        val topLevelReplies = mutableListOf<ReplyDto>()
        for (replyEntity in allReplies) {
            val currentReplyDto = replyMap[replyEntity.id]!!
            if (replyEntity.parentReplyId == null) {
                topLevelReplies.add(currentReplyDto)
            } else {
                val parentId = replyEntity.parentReplyId
                val parentReplyDto = replyMap[parentId]
                if (parentReplyDto != null) {
                    val currentChildren = parentReplyDto.replies ?: emptyList()
                    val updatedChildren = (currentChildren + currentReplyDto).toList()
                    replyMap[parentId] = parentReplyDto.copy(replies = updatedChildren)
                }
            }
        }
        return topLevelReplies.map { replyMap[it.id]!! }
    }


    @Transactional
    fun createComment(boardId: Long, request: CreateCommentRequest, requestUserId: String): ReplyDto {
        // findById 는 Optional 이라 null 이 될 수 없다. 예전 `?: throw` 는 절대 실행되지 않아서
        // 없는 게시글에 댓글을 달면 FK 위반 500 이 났다.
        val board = boardRepository.findById(boardId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다: $boardId") }
        if (board.isDeleted) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다: $boardId")
        }
        if (request.content.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "댓글 내용을 입력해주세요.")
        }

        // 작성자는 인증(로그인 토큰 또는 게스트 쿠키) 기준. 요청 본문의 userId/userName 은 쓰지 않는다.
        val author = userRepository.findByUserId(requestUserId)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "작성자를 확인할 수 없습니다.")

        val newReplyEntity: ReplyEntity

        if (request.parentId == null) {
            newReplyEntity = ReplyEntity(
                id = 0,
                postId = boardId,
                userId = author.userId,
                userName = author.name,
                title = null,
                content = request.content,
                insertDts = LocalDateTime.now(),
                parentReplyId = null
            )
        } else {
            val parentReplyEntity = replyRepository.findById(request.parentId)
                .orElseThrow { IllegalArgumentException("부모 댓글을 찾을 수 없습니다: ${request.parentId}") }

            newReplyEntity = ReplyEntity(
                id = 0,
                postId = parentReplyEntity.postId,
                userId = author.userId,
                userName = author.name,
                title = null,
                content = request.content,
                insertDts = LocalDateTime.now(),
                parentReplyId = request.parentId
            )
        }

        val savedReplyEntity = replyRepository.save(newReplyEntity)

        return savedReplyEntity.toReplyDto(boardId) // ReplyDto로 변환하여 반환!
    }

    // 댓글 수정
    @Transactional
    fun updateComment(boardId: Long, replyId: Int, request: UpdateCommentRequest, requestUserId: String): ReplyDto {
        // 1. 해당 게시글에 속하는 댓글인지 확인하고 조회
        val targetReply = replyRepository.findByPostIdAndId(boardId, replyId)
            .orElseThrow { IllegalArgumentException("해당 게시글($boardId)에 속하는 댓글($replyId)을 찾을 수 없습니다.") }
        // 작성자 본인만 수정 가능 (회원은 로그인 계정, 비회원은 게스트 쿠키의 계정)
        if (targetReply.userId != requestUserId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "댓글을 수정할 권한이 없습니다.")
        }

        // 2. 댓글 내용 및 수정일시 업데이트
        targetReply.apply {
            content = request.content // content 업데이트
            updateDts = LocalDateTime.now() // 수정일시 업데이트
        }

        // 3. 업데이트된 댓글 저장
        val updatedReply = replyRepository.save(targetReply) // save 호출

        // 4. 저장된 ReplyEntity를 ReplyDto로 변환하여 반환
        return updatedReply.toReplyDto(boardId)
    }

    // 댓글 삭제
    @Transactional
    fun deleteComment(boardId: Long, replyId: Int, requestUserId: String) {
        // 1. 해당 게시글에 속하는 댓글인지 확인하고 조회
        val targetReply = replyRepository.findByPostIdAndId(boardId, replyId)
            .orElseThrow { IllegalArgumentException("해당 게시글($boardId)에 속하는 댓글($replyId)을 찾을 수 없습니다.") }
        // 작성자 본인만 삭제 가능
        if (targetReply.userId != requestUserId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "댓글을 삭제할 권한이 없습니다.")
        }

        // 2. 댓글을 물리적으로 삭제
        // 이 때, `parent_reply_id`의 ON DELETE CASCADE 제약 조건에 의해 해당 댓글의 모든 자식 대댓글들도 DB에서 자동으로 삭제
        replyRepository.delete(targetReply)
    }
}

