package com.connect.service.setting.dto

/**
 * 앱 테마 설정. primary 가 null 이면 앱에 내장된 기본 색을 쓴다.
 */
data class ThemeSettingDto(
    val primary: String? = null
)
