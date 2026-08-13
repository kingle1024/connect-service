package com.connect.service.common

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.server.ResponseStatusException

@ControllerAdvice
class GlobalExceptionHandler {
    // NoSuchElementException이 발생했을 때 404 Not Found 반환
    @ExceptionHandler(NoSuchElementException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND) // HTTP 404 상태 코드 설정
    fun handleNoSuchElementException(ex: NoSuchElementException): ResponseEntity<String> {
        return ResponseEntity("요청하신 리소스를 찾을 수 없습니다: ${ex.message}", HttpStatus.NOT_FOUND)
    }

    // IllegalArgumentException이 발생했을 때 400 Bad Request 반환
    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // HTTP 400 상태 코드 설정
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<String> {
        return ResponseEntity("잘못된 요청입니다: ${ex.message}", HttpStatus.BAD_REQUEST)
    }

    // ResponseStatusException은 이미 상태 코드와 메시지를 들고 있으므로 그대로 전달한다.
    // 아래 Exception 핸들러가 먼저 잡으면 403/404까지 500으로 뭉개진다.
    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(ex: ResponseStatusException): ResponseEntity<String> {
        return ResponseEntity(ex.reason ?: "요청을 처리할 수 없습니다.", ex.statusCode)
    }

    // (선택 사항) 기타 모든 예외를 처리하는 핸들러 (더 명확한 메시지를 위해 필요 시 추가)
    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleGeneralException(ex: Exception): ResponseEntity<String> {
        return ResponseEntity("서버 내부 오류가 발생했습니다: ${ex.message}", HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
