package com.connect.service.comment

import com.connect.service.board.repository.BoardRepository
import com.connect.service.comment.domain.ReplyDto
import com.connect.service.comment.domain.ReplyEntity
import com.connect.service.comment.repository.ReplyRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.IllegalArgumentException
import com.connect.service.comment.domain.toReplyDto

@Service
@Transactional(readOnly = true) // 읽기 전용 트랜잭션, 쓰기는 @Transactional 별도 적용
class CommentService(
    private val replyRepository: ReplyRepository,
    private val commentMstRepository: CommentMstRepository,
    private val commentDtlRepository: CommentDtlRepository,
    private val boardRepository: BoardRepository // 게시글 존재 여부 확인용 (BoardRepository가 필요해)
) {

    fun getCommentsByBoardId(boardId: Long): List<ReplyDto> { // 반환 타입 변경!
        val allReplies = replyRepository.findAllByPostIdOrderByInsertDtsAsc(boardId)
        val replyMap = allReplies
            .associateBy { it.id }
            .mapValues { (_, entity: ReplyEntity) -> entity.toReplyDto(boardId) }
            .toMutableMap()

        val topLevelReplies = mutableListOf<ReplyDto>() // 타입 변경!
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
    fun updateComment(boardId: Long, commentId: Long, request: UpdateCommentRequest): CommentResponse {
        // boardId가 path variable에 있기 때문에 boardId에 속한 댓글/대댓글인지 확인해야 함.

        // CommentMst인지 CommentDtl인지 확인
        val commentMst = commentMstRepository.findByIdOrNull(commentId)
        if (commentMst != null && commentMst.postId == boardId) { // CommentMst이고 해당 boardId에 속한다면
            commentMst.apply {
                content = request.content
                updateDts = LocalDateTime.now()
            }
            val updatedComment = commentMstRepository.save(commentMst)
            return CommentResponse.from(updatedComment)
        }

        val commentDtl = commentDtlRepository.findByIdOrNull(commentId)
        if (commentDtl != null) { // CommentDtl인 경우, 부모 CommentMst가 해당 boardId에 속하는지 확인
            val parentComment = commentMstRepository.findByIdOrNull(commentDtl.parentId)
            if (parentComment != null && parentComment.postId == boardId) {
                commentDtl.apply {
                    content = request.content
                    updateDts = LocalDateTime.now()
                }
                val updatedReply = commentDtlRepository.save(commentDtl)
                return CommentResponse.from(updatedReply)
            }
        }

        throw IllegalArgumentException("해당 게시글($boardId)에 속하는 댓글을 찾을 수 없습니다: $commentId")
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

