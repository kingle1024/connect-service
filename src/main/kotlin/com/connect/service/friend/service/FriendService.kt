package com.connect.service.friend.service

import com.connect.service.friend.dto.FriendRequestProcessDto
import com.connect.service.friend.dto.FriendRequestReceivedResponse
import com.connect.service.friend.dto.FriendRequestSendDto
import com.connect.service.friend.dto.FriendResponse
import com.connect.service.friend.entity.FriendRequest
import com.connect.service.friend.entity.Friendship
import com.connect.service.friend.enum.FriendRequestStatus
import com.connect.service.friend.repository.FriendRequestRepository
import com.connect.service.friend.repository.FriendshipRepository
import com.connect.service.user.repository.UserRepository
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
class FriendService(
    private val userRepository: UserRepository,
    private val friendRequestRepository: FriendRequestRepository,
    private val friendshipRepository: FriendshipRepository
) {

    // --- 친구 추가 기능 ---
    @Transactional
    fun sendFriendRequest(currentUserId: String, request: FriendRequestSendDto): FriendRequest {
        // 닉네임 대신 user_id로 조회하도록 변경
        val receiver = userRepository.findByUserId(request.receiverUserId) // user_id로 조회
            ?: throw IllegalArgumentException("수신자 ID ${request.receiverUserId}의 사용자를 찾을 수 없습니다.")

        if (currentUserId == receiver.userId) {
            throw IllegalArgumentException("자기 자신에게 친구 요청을 보낼 수 없습니다.")
        }

        // 이미 친구 관계인지 확인 (application level validation)
        if (friendshipRepository.findByUserIdAndFriendId(currentUserId, receiver.userId) != null) {
            throw IllegalArgumentException("이미 친구인 사용자입니다.")
        }

        // 이미 보류 중인 요청이 있는지 확인
        if (friendRequestRepository.findBySenderIdAndReceiverId(currentUserId, receiver.userId) != null) {
            throw IllegalArgumentException("이미 친구 요청을 보냈거나 받은 요청이 존재합니다.")
        }
        // 상대방이 나에게 보낸 요청이 있는지 확인하고 처리하는 로직도 추가 가능

        val friendRequest = FriendRequest(
            senderId = currentUserId,
            receiverId = receiver.userId,
            status = FriendRequestStatus.PENDING
        )
        return friendRequestRepository.save(friendRequest)
    }

    @Transactional
    fun processFriendRequest(currentUserId: String, requestId: Long, processDto: FriendRequestProcessDto): FriendRequest {
        val friendRequest = friendRequestRepository.findByIdOrNull(requestId)
            ?: throw IllegalArgumentException("친구 요청을 찾을 수 없습니다.")

        if (friendRequest.receiverId != currentUserId) {
            throw IllegalArgumentException("해당 친구 요청을 처리할 권한이 없습니다.")
        }

        if (friendRequest.status != FriendRequestStatus.PENDING) {
            throw IllegalArgumentException("이미 처리된 친구 요청입니다.")
        }

        when (processDto.status.uppercase()) {
            "ACCEPTED" -> {
                friendRequest.status = FriendRequestStatus.ACCEPTED
                // Friendships 테이블에 양방향으로 추가 (user_id로)
                val friendship1 = Friendship(userId = friendRequest.senderId, friendId = friendRequest.receiverId)
                val friendship2 = Friendship(userId = friendRequest.receiverId, friendId = friendRequest.senderId)
                friendshipRepository.saveAll(listOf(friendship1, friendship2))
            }
            "REJECTED" -> {
                friendRequest.status = FriendRequestStatus.REJECTED
            }
            else -> throw IllegalArgumentException("유효하지 않은 처리 상태입니다: ${processDto.status}")
        }
        return friendRequestRepository.save(friendRequest)
    }

    fun getReceivedFriendRequests(currentUserId: String): List<FriendRequestReceivedResponse> {
        val requests = friendRequestRepository.findByReceiverIdAndStatus(currentUserId, FriendRequestStatus.PENDING)
        return requests.map { request ->
            val sender = userRepository.findByUserId(request.senderId) // user_id로 조회
                ?: throw IllegalStateException("요청을 보낸 사용자를 찾을 수 없습니다: ${request.senderId}")
            FriendRequestReceivedResponse(
                id = request.id,
                name = sender.name,
                senderUserId = request.senderId,
                receiverId = request.receiverId,
                status = request.status.name,
            )
        }
    }

    // --- 친구 삭제 기능 ---
    @Transactional
    fun deleteFriend(currentUserId: String, friendUserIdToDelete: String) {
        if (currentUserId == friendUserIdToDelete) {
            throw IllegalArgumentException("자기 자신을 친구 목록에서 삭제할 수 없습니다.")
        }

        val friendship1 = friendshipRepository.findByUserIdAndFriendId(currentUserId, friendUserIdToDelete)
        val friendship2 = friendshipRepository.findByUserIdAndFriendId(friendUserIdToDelete, currentUserId)

        if (friendship1 == null && friendship2 == null) {
            throw IllegalArgumentException("친구 관계를 찾을 수 없습니다.")
        }

        friendship1?.let { friendshipRepository.delete(it) }
        friendship2?.let { friendshipRepository.delete(it) }
    }

    // --- 친구 목록 조회 ---
    fun getFriends(currentUserId: String): List<FriendResponse> {
        val friendships = friendshipRepository.findByUserId(currentUserId)
        return friendships.map { friendship ->
            val friendUser = userRepository.findByUserId(friendship.friendId) // user_id로 조회
                ?: throw IllegalStateException("친구 사용자를 찾을 수 없습니다: ${friendship.friendId}")
            FriendResponse(
                friendshipId = friendship.id,
                friendUserId = friendship.friendId,
                friendNickname = friendUser.name, // users 테이블의 name 필드를 닉네임으로 사용
                isBlocked = friendship.isBlocked
            )
        }
    }

    // --- 대화하기 기능 (간단한 스텁) ---
    @Transactional
    fun createOneToOneChatRoom(currentUserId: String, participantUserId: String): Long {
        println("User $currentUserId and User $participantUserId are creating a one-to-one chat room.")
        return 123L
    }

    fun getChatRooms(currentUserId: String): List<String> {
        println("Getting chat rooms for user $currentUserId.")
        return listOf("Chat Room 1", "Chat Room 2")
    }

    fun sendMessage(senderUserId: String, roomId: Long, content: String): String {
        println("User $senderUserId sending message '$content' to room $roomId.")
        return "Message sent successfully."
    }
}
