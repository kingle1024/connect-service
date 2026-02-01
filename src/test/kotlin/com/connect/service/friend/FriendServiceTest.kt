package com.connect.service.friend

import com.connect.service.friend.dto.FriendRequestSendDto
import com.connect.service.friend.entity.FriendRequest
import com.connect.service.friend.enum.FriendRequestStatus
import com.connect.service.friend.repository.FriendRequestRepository
import com.connect.service.friend.repository.FriendshipRepository
import com.connect.service.friend.service.FriendService
import com.connect.service.user.domain.Users
import com.connect.service.user.repository.UserRepository
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.Test
import kotlin.test.assertEquals

@SpringBootTest
class FriendServiceTest {

    private val userRepository: UserRepository = mock()
    private val friendRequestRepository: FriendRequestRepository = mock()
    private val friendshipRepository: FriendshipRepository = mock()
    private val friendService = FriendService(userRepository, friendRequestRepository, friendshipRepository)

    @Test
    fun `정상 케이스 - 친구 요청 생성`() {
        val currentUserId = "userA"
        val receiverUserId = "userB"
        val requestDto = FriendRequestSendDto(receiverUserId)

        val receiverUser = Users(
            userId = receiverUserId,
            email = "receiver@example.com",
            name = "receiverName",
            rawPassword = "encodedPassword123"
        )

        // 모킹 설정
        whenever(userRepository.findByUserId(receiverUserId)).thenReturn(receiverUser)
        whenever(friendshipRepository.findByUserIdAndFriendId(currentUserId, receiverUserId)).thenReturn(null)
        whenever(friendRequestRepository.findBySenderIdAndReceiverId(currentUserId, receiverUserId)).thenReturn(null)
        whenever(friendRequestRepository.save(any())).thenAnswer { it.arguments[0] as FriendRequest }


        // 실제 서비스 호출
        val result = friendService.sendFriendRequest(currentUserId, requestDto)

        // 결과 검증
        assertEquals(currentUserId, result.senderId)
        assertEquals(receiverUserId, result.receiverId)
        assertEquals(FriendRequestStatus.PENDING, result.status)

        // Mock 호출 검증
        verify(userRepository).findByUserId(receiverUserId)
        verify(friendshipRepository).findByUserIdAndFriendId(currentUserId, receiverUserId)
        verify(friendRequestRepository).findBySenderIdAndReceiverId(currentUserId, receiverUserId)
        verify(friendRequestRepository).save(any())
    }
}
