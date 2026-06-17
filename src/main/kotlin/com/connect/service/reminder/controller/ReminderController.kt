package com.connect.service.reminder.controller

import com.connect.service.reminder.dto.ReminderCreateRequest
import com.connect.service.reminder.dto.ReminderResponse
import com.connect.service.reminder.service.ReminderService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/account/reminders") // /api/account/** 는 SecurityConfig 에서 허용되므로 별도 설정 불필요
@CrossOrigin
class ReminderController(
    private val reminderService: ReminderService
) {
    // 알림 등록
    @PostMapping
    fun create(
        @RequestBody request: ReminderCreateRequest,
        authentication: Authentication?
    ): ResponseEntity<Any> {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.")
        }
        return try {
            val saved = reminderService.create(authentication.name, request)
            ResponseEntity.ok(ReminderResponse.from(saved))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(e.message ?: "잘못된 요청입니다.")
        }
    }

    // 내 알림 목록
    @GetMapping
    fun list(authentication: Authentication?): ResponseEntity<List<ReminderResponse>> {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        val reminders = reminderService.list(authentication.name).map { ReminderResponse.from(it) }
        return ResponseEntity.ok(reminders)
    }

    // 알림 삭제
    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: Long,
        authentication: Authentication?
    ): ResponseEntity<Any> {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.")
        }
        reminderService.delete(authentication.name, id)
        return ResponseEntity.noContent().build()
    }
}
