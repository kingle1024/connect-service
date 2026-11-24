package com.connect.service.setting.entity

import com.connect.service.comment.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 백오피스에서 바꾸는 앱 전역 설정 (키-값).
 * 예) theme.primary = "#FF5A2F" (제품/브랜드 색)
 */
@Entity
@Table(name = "app_setting")
class AppSetting(
    @Id
    @Column(name = "setting_key", length = 100)
    val settingKey: String,

    @Column(name = "setting_value", nullable = false, length = 1000)
    var settingValue: String
) : BaseEntity()
