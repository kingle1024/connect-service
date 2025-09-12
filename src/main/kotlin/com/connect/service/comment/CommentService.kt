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
    private val commentRepository: CommentRepository,
    private val boardRepository: BoardRepository // 게시글 존재 여부 확인용 (BoardRepository가 필요해)
) {

    // 댓글 생성
    @Transactional
    fun createComment(boardId: Long, request: CreateCommentRequest): CommentResponse {
        // 게시글이 존재하는지 확인
        boardRepository.findByIdOrNull(boardId)
            ?: throw IllegalArgumentException("게시글을 찾을 수 없습니다: $boardId")

        // 부모 댓글 ID가 있다면 존재하는 댓글인지 확인
        request.parentCommentId?.let { parentId ->
            commentRepository.findByIdOrNull(parentId)
                ?: throw IllegalArgumentException("부모 댓글을 찾을 수 없습니다: $parentId")
        }

        val newComment = CommentMst(
            content = request.content,
            author = request.author,
            boardId = boardId,
            parentCommentId = request.parentCommentId
        )
        val savedComment = commentRepository.save(newComment)
        return CommentResponse.from(savedComment)
    }

    // 특정 게시글의 댓글들을 계층형으로 조회
    fun getCommentsByBoardId(boardId: Long): List<CommentResponse> {
        val allComments = commentRepository.findAllByBoardIdOrderByInsertDtsAsc(boardId)
        val commentMap = allComments.associateBy { it.id!! } // ID를 키로 하는 맵 생성

        val topLevelComments = mutableListOf<CommentResponse>()

        // 맵 순회하면서 대댓글들 부모에 연결
        for (comment in allComments) {
            val commentResponse = CommentResponse.from(comment)
            if (comment.parentCommentId == null) {
                topLevelComments.add(commentResponse) // 최상위 댓글은 바로 추가
            } else {
                commentMap[comment.parentCommentId]?.let { parentComment ->
                    // 부모 댓글이 '삭제됨' 상태이고, 부모 댓글이 최상위 댓글인 경우 직접 처리
                    // (여기서는 CommentResponse.replies에 추가하도록 구현)
                    commentMap[comment.parentCommentId]?.let {
                        // 실제 CommentResponse 맵에 부모 찾아서 추가하는 로직이 필요.
                        // Map<Long, CommentResponse>를 만들어서 사용하면 효율적이야.
                        // 아래 계층 구조를 만드는 로직을 사용하면 좀 더 깔끔해!
                    }
                }
            }
        }
        return buildCommentHierarchy(topLevelComments, allComments)
    }

    // 댓글 계층 구조를 재귀적으로 만드는 함수
    private fun buildCommentHierarchy(
        topLevelComments: List<CommentResponse>,
        allComments: List<CommentMst>
    ): List<CommentResponse> {
        val commentResponseMap = allComments.associate { it.id!! to CommentResponse.from(it) }
        val rootComments = mutableListOf<CommentResponse>()

        for (comment in allComments) {
            val commentResponse = commentResponseMap[comment.id!!]!!
            if (comment.parentCommentId == null) {
                rootComments.add(commentResponse)
            } else {
                commentResponseMap[comment.parentCommentId]?.replies?.add(commentResponse)
            }
        }

        // insertDts 순으로 정렬해서 반환 (optional, DB 쿼리에서 이미 정렬되어 있다고 가정)
        rootComments.sortBy { it.insertDts }
        rootComments.forEach { sortRepliesRecursively(it.replies) } // 대댓글도 정렬
        return rootComments
    }

    private fun sortRepliesRecursively(replies: MutableList<CommentResponse>) {
        replies.sortBy { it.insertDts }
        replies.forEach { sortRepliesRecursively(it.replies) }
    }


    // 댓글 수정
    @Transactional
    fun updateComment(commentId: Long, request: UpdateCommentRequest): CommentResponse {
        val comment = commentRepository.findByIdOrNull(commentId)
            ?: throw IllegalArgumentException("댓글을 찾을 수 없습니다: $commentId")

        comment.apply {
            content = request.content
            updateDts = LocalDateTime.now()
        }
        val updatedComment = commentRepository.save(comment)
        return CommentResponse.from(updatedComment)
    }

    // 댓글 삭제
    @Transactional
    fun deleteComment(commentId: Long) {
        val comment = commentRepository.findByIdOrNull(commentId)
            ?: throw IllegalArgumentException("댓글을 찾을 수 없습니다: $commentId")

        // 만약 대댓글이 있다면 바로 삭제하지 않고 '삭제됨' 상태로 변경
        // (orphanRemoval=true 같은 JPA 설정으로 자식 댓글이 자동으로 삭제되게 할 수도 있어)
        if (commentRepository.findAllByParentCommentIdOrderByInsertDtsAsc(commentId).isNotEmpty()) {
            comment.isDeleted = true // 논리적 삭제
            commentRepository.save(comment)
        } else {
            // 대댓글이 없는 경우, 바로 삭제
            commentRepository.delete(comment)
            // 만약 삭제된 부모 댓글에 더 이상 대댓글이 없다면 부모 댓글도 완전 삭제 (선택 사항)
            comment.parentCommentId?.let { parentId ->
                val parentComment = commentRepository.findByIdOrNull(parentId)
                if (parentComment != null && parentComment.isDeleted &&
                    commentRepository.findAllByParentCommentIdOrderByInsertDtsAsc(parentId).isEmpty()) {
                    commentRepository.delete(parentComment)
                }
            }
        }
    }
}

