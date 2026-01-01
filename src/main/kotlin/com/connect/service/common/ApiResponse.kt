package com.connect.service.common

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
) {
    companion object {
        fun <T> success(message: String = "성공", data: T? = null) = ApiResponse(true, message, data)
        fun <T> error(message: String = "실패", data: T? = null) = ApiResponse(false, message, data)
    }
}
