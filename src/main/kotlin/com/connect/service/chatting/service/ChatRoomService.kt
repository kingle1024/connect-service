package com.connect.service.chatting.service

import com.connect.service.chatting.dto.ChatOneToOneRoomDto
import com.connect.service.chatting.dto.ChatRoomDto
import com.connect.service.chatting.dto.ChatRoomParticipantRes
import com.connect.service.chatting.repository.RoomMembershipRepository
import com.connect.service.chatting.entity.ChatRoom
import com.connect.service.chatting.entity.RoomMembership
import com.connect.service.chatting.entity.RoomMembershipId
import com.connect.service.chatting.enums.RoomType
import com.connect.service.chatting.repository.ChatRoomRepository
import com.connect.service.user.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Service
class ChatRoomService (
    private val chatRoomRepository: ChatRoomRepository,
    private val roomMembershipRepository: RoomMembershipRepository,
    private val userRepository: UserRepository
){
    // roomId를 키로 ChatRoom 객체를 저장할 맵
    private val chatRooms: ConcurrentHashMap<String, ChatRoom> = ConcurrentHashMap()

    @Transactional // 이 메서드 전체를 하나의 DB 트랜잭션으로 묶음
    fun addParticipant(
        roomId: String,
        userId: String,
        roomName: String?,
        roomType: String?
    ): Boolean {
       // 1. ChatRoom 존재 여부 확인 및 생성
       // findById(roomId)로 방을 찾고, 없으면 orElseGet{} 람다식을 실행하여 새 방을 생성
       val chatRoom = chatRoomRepository.findById(roomId).orElseGet {
           // 방이 없으면 새로운 방을 생성하고 초기 정보 설정
           val newRoom = ChatRoom(
               roomId = roomId,
               roomType = (roomType ?: RoomType.GROUP) as String,
               leaderUserId = userId, // 방장을 방을 처음 생성한 유저로 설정
               roomName = roomName,
               createdAt = LocalDateTime.now() // 생성 시간 설정
           )
           chatRoomRepository.save(newRoom) // 생성된 새 방을 DB에 저장
       }

       // 2. RoomMembership 존재 여부 확인 및 생성
       val membershipId = RoomMembershipId(userId, roomId) // 사용자 ID와 방 ID로 복합 키 생성
       val exists = roomMembershipRepository.findById(membershipId) // 해당 유저가 이미 이 방의 멤버인지 DB에서 확인

       if (exists.isEmpty) { // 아직 멤버가 아니라면
           val newMembership = RoomMembership(
               id = membershipId, // 복합 키
               chatRoom = chatRoom, // 관계 매핑된 ChatRoom 엔티티
               roomName = roomName,
               joinedAt = LocalDateTime.now() // 참여 시간 기록
           )
           roomMembershipRepository.save(newMembership) // 새로운 멤버십을 DB에 저장
           return true // 성공적으로 새로 추가됨을 반환
       }
       return false // 이미 존재하는 멤버임을 반환
    }

    // 💡 (수정!) 유저가 채팅방에서 나갈 때 호출됩니다. (멤버십 삭제)
    @Transactional // 이 메서드 전체를 하나의 DB 트랜잭션으로 묶음
    fun removeParticipant(roomId: String, userId: String): Boolean {
       val membershipId = RoomMembershipId(userId, roomId) // 멤버십 복합 키 생성
       val exists = roomMembershipRepository.existsById(membershipId) // 해당 멤버십이 DB에 존재하는지 확인

       if (exists) { // 멤버십이 존재한다면
           roomMembershipRepository.deleteById(membershipId) // DB에서 해당 멤버십을 삭제

           // 🚨 (옵션) 방에 아무도 없으면 방 자체를 삭제하는 로직
           val remainingMembers = roomMembershipRepository.countByIdRoomId(roomId) // 해당 방에 남아있는 멤버 수 조회
           if (remainingMembers == 0L) { // 남은 멤버가 0명이라면 (모두 나갔다면)
               chatRoomRepository.deleteById(roomId) // 해당 채팅방 자체를 DB에서 삭제
           }
           return true // 성공적으로 제거됨을 반환
       }
       return false // 멤버십이 없었음을 반환
    }

    @Transactional(readOnly = true) // 데이터 변경 없이 읽기만 하므로 읽기 전용 트랜잭션으로 설정 (성능 최적화)
    fun getRoomsForUser(userId: String): List<ChatRoomDto> {
       // 해당 userId를 가진 모든 RoomMembership 엔티티를 DB에서 조회
       return roomMembershipRepository.findByIdUserId(userId)
           .map { membership -> // 조회된 각 멤버십에 대해 DTO로 변환
               val chatRoom = membership.chatRoom // 멤버십에 연결된 ChatRoom 엔티티 가져오기
               // 해당 방의 현재 참여자 수를 DB에서 조회 (최신 정보)
               val participantsCount = roomMembershipRepository.countByIdRoomId(chatRoom.roomId)
               // ChatRoomDto 객체로 변환하여 반환
               ChatRoomDto(
                   id = chatRoom.roomId,
                   name = chatRoom.roomName,
                   leaderId = chatRoom.leaderUserId,
                   participantsCount = participantsCount
               )
           }.toList() // List<ChatRoomDto>로 최종 반환
    }

    @Transactional(readOnly = true)
    fun getRoomsForRoomId(roomId: String): List<ChatRoomParticipantRes> {
       val memberships = roomMembershipRepository.findByIdRoomId(roomId)
       // 참여자 표시 이름을 한 번에 조회한다
       val names = userRepository.findByUserIdIn(memberships.map { it.id.userId }.distinct())
           .filter { it.name.isNotBlank() }
           .associate { it.userId to it.name }
       return memberships.map { membership ->
               val chatRoom = membership.chatRoom
               ChatRoomParticipantRes(
                   roomId = chatRoom.roomId,
                   userId = membership.id.userId,
                   name = names[membership.id.userId],
                   leader = chatRoom.leaderUserId == membership.id.userId
               )
           }.toList()
    }



    fun findByRoomId(roomId: String): ChatRoom? {
        val chatRoom = chatRoomRepository.findById(roomId)
        return if(chatRoom.isPresent) {
            chatRoom.get()
        } else {
            null
        }
    }

    @Transactional(readOnly = true)
    fun getOneToOneRoomsForUser(userId: String): List<ChatOneToOneRoomDto> {
        return chatRoomRepository.findOneToOneRoomsByUserId(userId)
    }

    // 방장 확인 로직
    fun isRoomLeader(roomId: String, userId: String): Boolean {
       // ChatRoom을 찾아서, 그 방의 leaderUserId와 요청한 userId가 같은지 확인
       return chatRoomRepository.findById(roomId).map { it.leaderUserId == userId }.orElse(false)
    }

    // 채팅방 존재 여부 확인
    fun doesRoomExist(roomId: String): Boolean {
       return chatRoomRepository.existsById(roomId)
    }

     fun getRoom(roomId: String): ChatRoom? {
         return chatRoomRepository.findById(roomId).orElse(null)
     }

    @Transactional
    fun updateRoomName(roomId: String, newRoomName: String): ChatRoom? {
        // roomId로 ChatRoom 엔티티를 찾습니다.
        val chatRoom = chatRoomRepository.findByIdOrNull(roomId) // @Id로 지정된 roomId 필드를 사용하여 찾음

        return if (chatRoom != null) {
            // 방을 찾았다면 이름을 업데이트합니다.
            chatRoom.roomName = newRoomName // ⬅️ ChatRoom 엔티티의 roomName 필드에 대입
            chatRoomRepository.save(chatRoom) // 변경된 엔티티 저장
        } else {
            null // 해당하는 방이 없는 경우
        }
    }
}
