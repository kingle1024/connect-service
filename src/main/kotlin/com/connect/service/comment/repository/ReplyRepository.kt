package com.connect.service.comment.repository

import com.connect.service.comment.domain.ReplyEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface ReplyRepository : JpaRepository<ReplyEntity, Int>{
    fun findAllByPostIdOrderByInsertDtsAsc(postId: Long): List<ReplyEntity>
    fun findByPostIdAndId(postId: Long, id: Int): Optional<ReplyEntity>
}
