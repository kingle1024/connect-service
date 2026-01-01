package com.connect.service.user.controller

import com.connect.service.common.ApiResponse
import com.connect.service.user.dto.EmailRequest
import com.connect.service.user.dto.ResetPasswordRequest
import com.connect.service.user.dto.VerificationRequest
import com.connect.service.user.service.AccountService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/account")
@CrossOrigin
class AccountController (
    private val accountService: AccountService
){
    /**
     * 비밀번호 찾기: 인증번호 발송 요청
     * 클라이언트에서 email을 받아 인증번호를 이메일로 전송합니다.
     * POST /api/account/find-password/send-code
     */
    @PostMapping("/find-password/send-code")
    fun sendVerificationCode(@RequestBody request: EmailRequest): ResponseEntity<ApiResponse<Unit>> {
        return try {
            accountService.sendVerificationCode(request.email)
            ResponseEntity.ok(ApiResponse.success("인증번호가 이메일로 발송되었습니다."))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "잘못된 요청입니다."))
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(ApiResponse.error("인증번호 발송에 실패했습니다. 잠시 후 다시 시도해주세요."))
        }
    }

    /**
     * 비밀번호 찾기: 인증번호 확인 요청
     * 클라이언트에서 email과 전송받은 코드를 받아 유효성을 검증합니다.
     * POST /api/account/find-password/verify-code
     */
    @PostMapping("/find-password/verify-code")
    fun verifyCode(@RequestBody request: VerificationRequest): ResponseEntity<ApiResponse<Unit>> {
        return try {
            if (accountService.verifyCode(request.email, request.code)) {
                ResponseEntity.ok(ApiResponse.success("인증번호가 확인되었습니다."))
            } else {
                ResponseEntity.badRequest().body(ApiResponse.error("인증번호가 일치하지 않거나 만료되었습니다."))
            }
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(ApiResponse.error("인증번호 확인 중 오류가 발생했습니다."))
        }
    }

    /**
     * 비밀번호 찾기: 비밀번호 재설정 요청
     * 인증번호 확인이 완료된 후, 클라이언트에서 email과 새로운 비밀번호를 받아 업데이트합니다.
     * POST /api/account/find-password/reset-password
     */
    @PostMapping("/find-password/reset-password")
    fun resetPassword(@RequestBody request: ResetPasswordRequest): ResponseEntity<ApiResponse<Unit>> {
        if (request.newPassword.length < 8 || !request.newPassword.contains(Regex("[a-zA-Z]")) ||
            !request.newPassword.contains(Regex("[0-9]")) || !request.newPassword.contains(Regex("[!@#\$%^&*()]"))) {
            return ResponseEntity.badRequest().body(ApiResponse.error("비밀번호는 8자 이상이며, 영문, 숫자, 특수문자를 포함해야 합니다."))
        }

        return try {
            if (accountService.resetPassword(request.email, request.newPassword)) {
                ResponseEntity.ok(ApiResponse.success("비밀번호가 성공적으로 재설정되었습니다."))
            } else {
                // 이메일이 서비스에 없거나 다른 이유로 실패할 경우
                ResponseEntity.badRequest().body(ApiResponse.error("비밀번호 재설정에 실패했습니다. 이메일을 다시 확인해주세요."))
            }
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(ApiResponse.error("비밀번호 재설정 중 오류가 발생했습니다."))
        }
    }
}
