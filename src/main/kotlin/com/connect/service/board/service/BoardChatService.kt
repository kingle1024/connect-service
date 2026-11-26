package com.connect.service.board.service

import com.connect.service.board.entity.BoardMst
import com.connect.service.board.repository.BoardParticipantRepository
import com.connect.service.board.repository.BoardRepository
import com.connect.service.chatting.enums.RoomType
import com.connect.service.chatting.repository.ChatRoomRepository
import com.connect.service.chatting.service.ChatRoomService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

/**
 * 모집 글마다 붙는 단체 채팅방.
 *
 * 방 ID 는 글 번호로 정해진다 (board_{글번호}) — 글 테이블에 컬럼을 늘리지 않고도 글 ↔ 방을 찾을 수 있다.
 *  - 글을 올리면 방을 만들고 작성자가 방장이 된다
 *  - 모집에 참여하면 방에 들어가고, 참여를 취소하면 방에서 빠진다
 *  - 방장(작성자)은 글이 살아 있는 동안 방을 나갈 수 없다 (ChatWebSocketController.leaveUser)
 * 이 기능 전에 올라온 글은 처음 '모집 채팅방'을 열 때 방을 만들고 작성자·참여자를 넣는다 (ensureRoom).
 */
@Service
class BoardChatService(
    private val boardRepository: BoardRepository,
    private val participantRepository: BoardParticipantRepository,
    private val chatRoomRepository: ChatRoomRepository,
    private val chatRoomService: ChatRoomService,
) {
    data class BoardRoom(val roomId: String, val roomName: String)

    /** 글을 올릴 때: 방을 만들고 작성자를 방장으로 넣는다. */
    @Transactional
    fun createRoom(board: BoardMst) {
        chatRoomService.addParticipant(roomIdOf(board.id!!), board.userId, roomNameOf(board), RoomType.GROUP.toString())
    }

    /** 모집 참여: 방에 넣는다 (방이 없으면 작성자·기존 참여자와 함께 만든다). */
    @Transactional
    fun addMember(board: BoardMst, userId: String) {
        ensureRoomWithMembers(board)
        chatRoomService.addParticipant(roomIdOf(board.id!!), userId, roomNameOf(board), RoomType.GROUP.toString())
    }

    /** 참여 취소: 방에서 뺀다. 방장은 빼지 않는다. */
    @Transactional
    fun removeMember(board: BoardMst, userId: String) {
        if (userId == board.userId) return
        chatRoomService.removeParticipant(roomIdOf(board.id!!), userId)
    }

    /**
     * '모집 채팅방' 열기: 작성자나 참여자만 들어갈 수 있다. 방이 없으면(예전 글) 이때 만든다.
     */
    @Transactional
    fun openRoom(boardId: Long, userId: String): BoardRoom {
        val board = boardRepository.findById(boardId)
            .filter { !it.isDeleted }
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "모집 글을 찾을 수 없습니다.") }
        val isMember = board.userId == userId || participantRepository.existsByBoardIdAndUserId(boardId, userId)
        if (!isMember) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "모집에 참여한 사람만 채팅방에 들어갈 수 있어요.")
        }
        ensureRoomWithMembers(board)
        chatRoomService.addParticipant(roomIdOf(board.id!!), userId, roomNameOf(board), RoomType.GROUP.toString())
        val room = chatRoomRepository.findById(roomIdOf(board.id!!)).get()
        return BoardRoom(room.roomId, room.roomName ?: roomNameOf(board))
    }

    /** 방장이 나갈 수 없는 모집 방인가 (글이 살아 있는 모집 채팅방) */
    @Transactional(readOnly = true)
    fun isActiveBoardRoom(roomId: String): Boolean {
        val boardId = boardIdOf(roomId) ?: return false
        return boardRepository.findById(boardId).map { !it.isDeleted }.orElse(false)
    }

    // 방이 없으면 작성자를 방장으로 만들고, 이미 참여한 사람들도 넣는다
    private fun ensureRoomWithMembers(board: BoardMst) {
        val roomId = roomIdOf(board.id!!)
        if (chatRoomRepository.existsById(roomId)) return
        val name = roomNameOf(board)
        chatRoomService.addParticipant(roomId, board.userId, name, RoomType.GROUP.toString())
        participantRepository.findAllByBoardIdOrderByIdAsc(board.id!!).forEach {
            chatRoomService.addParticipant(roomId, it.userId, name, RoomType.GROUP.toString())
        }
    }

    private fun roomNameOf(board: BoardMst) = board.title.take(100)

    companion object {
        private const val PREFIX = "board_"
        fun roomIdOf(boardId: Long) = "$PREFIX$boardId"
        fun boardIdOf(roomId: String): Long? =
            if (roomId.startsWith(PREFIX)) roomId.removePrefix(PREFIX).toLongOrNull() else null
    }
}
