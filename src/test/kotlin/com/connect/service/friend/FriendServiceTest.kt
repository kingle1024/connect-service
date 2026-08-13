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
import org.mockito.kotlin.doAnswer
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
        // save 의 반환 타입은 플랫폼 타입(FriendRequest!)이라 whenever(mock.save(any())) 형태로 감싸면
        // 스터빙 시점에 목이 돌려주는 null 에 코틀린 null 체크가 걸려 NPE 가 난다. doAnswer 로 스터빙한다.
        doAnswer { it.arguments[0] as FriendRequest }.whenever(friendRequestRepository).save(any())


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
