package com.connect.service.comment

import com.connect.service.board.repository.BoardRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.IllegalArgumentException

@Service
@Transactional(readOnly = true) // 읽기 전용 트랜잭션, 쓰기는 @Transactional 별도 적용
class CommentService(
    private val commentMstRepository: CommentMstRepository,
    private val commentDtlRepository: CommentDtlRepository,
    private val boardRepository: BoardRepository // 게시글 존재 여부 확인용 (BoardRepository가 필요해)
) {

    // 댓글 생성
    @Transactional
    fun createComment(boardId: Long, request: CreateCommentRequest): CommentResponse {
        // 게시글이 존재하는지 확인
        boardRepository.findByIdOrNull(boardId)
            ?: throw IllegalArgumentException("게시글을 찾을 수 없습니다: $boardId")

        // 2. 부모 댓글 ID 유무에 따라 CommentMst 또는 CommentDtl 생성
        if (request.parentId == null) {
            // 일반 댓글 생성 (CommentMst)
            val newCommentMst = CommentMst(
                userId = request.userId,
                userName = request.userName,
                content = request.content,
                postId = boardId // CommentMst는 postId를 가짐
            )
            val savedCommentMst = commentMstRepository.save(newCommentMst)
            return CommentResponse.from(savedCommentMst)
        } else {
            // 대댓글 생성 (CommentDtl)
            val parentComment = commentMstRepository.findByIdOrNull(request.parentId)
                ?: throw IllegalArgumentException("부모 댓글을 찾을 수 없습니다: ${request.parentId}")

            val newCommentDtl = CommentDtl(
                userId = request.userId,
                userName = request.userName,
                content = request.content,
                parentId = parentComment.id!! // CommentDtl은 parentId를 가짐
            )
            val savedCommentDtl = commentDtlRepository.save(newCommentDtl)

            // 양방향 관계 업데이트 (JPA 연관 관계를 통해 처리)
            // CommentMst의 replies 리스트에 CommentDtl 추가
            parentComment.replies.add(savedCommentDtl)
            commentMstRepository.save(parentComment) // 변경 사항 반영

            return CommentResponse.from(savedCommentDtl)
        }
    }

    // 특정 게시글의 댓글들을 계층형으로 조회
    fun getCommentsByBoardId(boardId: Long): List<CommentResponse> {
        // 해당 게시글의 모든 최상위 댓글 (CommentMst) 조회
        val topLevelComments = commentMstRepository.findAllByPostIdOrderByInsertDtsAsc(boardId)

        // CommentResponse로 변환 및 맵 생성
        val commentResponseMap = topLevelComments.associate { it.id!! to CommentResponse.from(it) }.toMutableMap()

        // 각 최상위 댓글의 대댓글 (CommentDtl)을 조회하고 CommentResponse에 연결
        for (commentMst in topLevelComments) {
            val commentMstResponse = commentResponseMap[commentMst.id!!]!!
            // CommentMst에 연결된 CommentDtl들은 JPA 연관 관계 설정에 따라 FetchType.LAZY로 가져오거나
            // commentDtlRepository를 통해 직접 조회 가능 (여기서는 직접 조회 예시)
            val childReplies = commentDtlRepository.findAllByParentIdOrderByInsertDtsAsc(commentMst.id!!)
            childReplies.map { CommentResponse.from(it) }.forEach { commentMstResponse.replies.add(it) }
        }

        return topLevelComments.map { commentResponseMap[it.id!!]!! }.toList()
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

