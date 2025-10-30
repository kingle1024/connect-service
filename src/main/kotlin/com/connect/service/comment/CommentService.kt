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

@Service
@Transactional(readOnly = true)
class CommentService(
    private val replyRepository: ReplyRepository,
    private val boardRepository: BoardRepository
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
    fun createComment(boardId: Long, request: CreateCommentRequest): ReplyDto { // 반환 타입 변경!
        boardRepository.findById(boardId)
            ?: throw IllegalArgumentException("게시글을 찾을 수 없습니다: $boardId")

        val newReplyEntity: ReplyEntity

        if (request.parentId == null) {
            newReplyEntity = ReplyEntity(
                id = 0,
                postId = boardId,
                userId = request.userId,
                userName = request.userName,
                title = request.title,
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
                userId = request.userId,
                userName = request.userName,
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
    fun updateComment(boardId: Long, replyId: Int, request: UpdateCommentRequest): ReplyDto {
        // 1. 해당 게시글에 속하는 댓글인지 확인하고 조회
        val targetReply = replyRepository.findByPostIdAndId(boardId, replyId)
            .orElseThrow { IllegalArgumentException("해당 게시글($boardId)에 속하는 댓글($replyId)을 찾을 수 없습니다.") }

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
    fun deleteComment(boardId: Long, replyId: Int) {
        // 1. 해당 게시글에 속하는 댓글인지 확인하고 조회
        val targetReply = replyRepository.findByPostIdAndId(boardId, replyId)
            .orElseThrow { IllegalArgumentException("해당 게시글($boardId)에 속하는 댓글($replyId)을 찾을 수 없습니다.") }

        // 2. 댓글을 물리적으로 삭제
        // 이 때, `parent_reply_id`의 ON DELETE CASCADE 제약 조건에 의해 해당 댓글의 모든 자식 대댓글들도 DB에서 자동으로 삭제
        replyRepository.delete(targetReply)
    }
}

