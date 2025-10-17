package com.connect.service.comment

import com.connect.service.board.repository.BoardRepository
import com.connect.service.comment.domain.CommentEntity
import com.connect.service.comment.domain.Reply
import com.connect.service.comment.repository.CommentRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.IllegalArgumentException

@Service
@Transactional(readOnly = true) // 읽기 전용 트랜잭션, 쓰기는 @Transactional 별도 적용
class CommentService(
    private val commentRepository: CommentRepository,
    private val commentMstRepository: CommentMstRepository,
    private val commentDtlRepository: CommentDtlRepository,
    private val boardRepository: BoardRepository // 게시글 존재 여부 확인용 (BoardRepository가 필요해)
) {

    // 댓글 생성
    @Transactional
    fun createComment(boardId: Long, request: CreateCommentRequest): Reply {
        // 1. 게시글이 존재하는지 확인
        boardRepository.findById(boardId)
            ?: throw IllegalArgumentException("게시글을 찾을 수 없습니다: $boardId")

        val newCommentEntity: CommentEntity
        val commentId: Int = (commentRepository.findMaxId() ?: 0) + 1 // 가상의 ID 생성 로직 (DB Auto-increment 사용 권장)

        if (request.parentId == null) {
            // 2.1. 최상위 댓글 생성
            newCommentEntity = CommentEntity(
                id = commentId,
                postId = boardId, // 최상위 댓글은 boardId에 직접 연결
                userId = request.userId,
                userName = request.userName,
                title = request.title, // 최상위 댓글은 title을 가질 수 있음
                content = request.content,
                insertDts = LocalDateTime.now(),
                parentReplyId = null // 최상위 댓글이므로 부모 댓글 ID는 null
            )
        } else {
            // 2.2. 대댓글 생성
            // 부모 댓글의 존재 여부 및 postId를 확인하기 위해 부모 댓글을 조회합니다.
            // CommentRepository에서 findById는 Long 타입 id를 받아야 함 (CommentEntity의 id가 Int이므로 캐스팅)
            val parentCommentEntity = commentRepository.findById(request.parentId)
                ?: throw IllegalArgumentException("부모 댓글을 찾을 수 없습니다: ${request.parentId}")

            newCommentEntity = CommentEntity(
                id = commentId,
                postId = parentCommentEntity.postId, // 대댓글은 부모 댓글과 동일한 postId를 가짐
                userId = request.userId,
                userName = request.userName,
                title = null, // 대댓글은 title이 없음
                content = request.content,
                insertDts = LocalDateTime.now(),
                parentReplyId = request.parentId // 대댓글의 부모 댓글 ID 셋팅
            )
        }

        val savedCommentEntity = commentRepository.save(newCommentEntity) // DB에 저장

        // 저장된 CommentEntity를 Reply DTO로 변환하여 반환
        return savedCommentEntity.toReplyDto(boardId)
    }

    // 특정 게시글의 댓글들을 계층형으로 조회
    fun getCommentsByBoardId(boardId: Long): List<Reply> {
        val allComments = commentRepository.findAllByPostIdOrderByInsertDtsAsc(boardId)

        // Map<댓글ID, Reply객체> 형태로 변환. toReplyDto 호출 시 boardId 전달.
        val replyMap = allComments
                    .associateBy { it.id } // Map의 키는 댓글 ID
                    .mapValues { (_, entity) -> entity.toReplyDto(boardId) } // Map의 값은 Reply DTO
                    .toMutableMap()

        val topLevelReplies = mutableListOf<Reply>()

        for (commentEntity in allComments) {
            val replyDto = replyMap[commentEntity.id]!! // 현재 댓글에 해당하는 Reply 객체

            if (commentEntity.parentReplyId == null) { // CommentEntity의 parentReplyId가 null이면 최상위 댓글
                topLevelReplies.add(replyDto)
            } else {
                // 대댓글인 경우, 부모 댓글의 replies 리스트에 현재 대댓글 추가
                val parentReply = replyMap[commentEntity.parentReplyId]
                if (parentReply != null) {
                    val currentReplies = parentReply.replies ?: emptyList()
                    val updatedReplies = (currentReplies + replyDto).toMutableList()

                    // 불변성 유지를 위해 copy를 사용하여 새로운 Reply 객체 생성 및 map 업데이트
                    replyMap[commentEntity.parentReplyId] = parentReply.copy(replies = updatedReplies)
                }
            }
        }

        // 최종 결과 반환: topLevelReplies에 추가된 객체들이 map에서 최종적으로 업데이트된 상태를 반영하도록 재참조.
        // 또는 그냥 topLevelReplies 자체를 반환해도 됨 (만약 topLevelReplies에 들어간 Reply 객체가 map의 참조와 동일하다면)
        // 여기서는 topLevelReplies 자체를 반환하면서 정렬만 해줍니다.
        return topLevelReplies.sortedBy { it.insertDts }
    }

private fun CommentEntity.toReplyDto(boardId: Long): Reply {
        return Reply(
            id = this.id,
            userId = this.userId,
            userName = this.userName,
            title = this.title,
            content = this.content,
            insertDts = this.insertDts.format(DateTimeFormatter.ISO_DATE_TIME),
            // 사용자님의 요청에 따라 parentId 셋팅:
            // parentReplyId가 null (최상위 댓글)이면 boardId로,
            // 아니면 CommentEntity의 parentReplyId로 셋팅합니다.
            parentId = this.parentReplyId ?: boardId.toInt(), // Long을 Int로 캐스팅
            replies = null // 초기에는 비워두고, 계층 구조 생성 시 채워넣음
        )
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
fun deleteComment(boardId: Long, commentId: Long) {
        // CommentMst인지 CommentDtl인지 확인
        val commentMst = commentMstRepository.findByIdOrNull(commentId)
        if (commentMst != null && commentMst.postId == boardId) { // CommentMst이고 해당 boardId에 속한다면
            // 만약 대댓글(CommentDtl)이 남아있다면 논리적 삭제
            if (commentDtlRepository.countByParentIdAndIsDeletedFalse(commentMst.id!!) > 0) {
                commentMst.isDeleted = true // 논리적 삭제
                commentMst.updateDts = LocalDateTime.now()
                commentMstRepository.save(commentMst)
            } else {
                commentMstRepository.delete(commentMst) // 대댓글 없으면 바로 삭제
            }
            return
        }

        val commentDtl = commentDtlRepository.findByIdOrNull(commentId)
        if (commentDtl != null) { // CommentDtl인 경우, 부모 CommentMst가 해당 boardId에 속하는지 확인
            val parentComment = commentMstRepository.findByIdOrNull(commentDtl.parentId)
            if (parentComment != null && parentComment.postId == boardId) {
                // 대댓글은 일반적으로 바로 삭제 (논리적 삭제 대신)
                commentDtlRepository.delete(commentDtl)

                // 대댓글이 삭제된 후, 부모 댓글이 논리적 삭제 상태이고 더 이상 자식 대댓글이 없다면 부모 댓글도 완전 삭제 (선택 사항)
                if (parentComment.isDeleted &&
                    commentDtlRepository.countByParentIdAndIsDeletedFalse(parentComment.id!!) == 0L
                ) {
                    commentMstRepository.delete(parentComment)
                }
                return
            }
        }

        throw IllegalArgumentException("해당 게시글($boardId)에 속하는 댓글을 찾을 수 없습니다: $commentId")
    }

}

