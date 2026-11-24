package com.connect.service.setting.service

import com.connect.service.setting.dto.ThemeSettingDto
import com.connect.service.setting.entity.AppSetting
import com.connect.service.setting.repository.AppSettingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AppSettingService(
    private val appSettingRepository: AppSettingRepository
) {
    companion object {
        const val THEME_PRIMARY = "theme.primary"
        private val HEX_COLOR = Regex("^#[0-9A-Fa-f]{6}$")
    }

    @Transactional(readOnly = true)
    fun getTheme(): ThemeSettingDto =
        ThemeSettingDto(primary = appSettingRepository.findById(THEME_PRIMARY).orElse(null)?.settingValue)

    /**
     * 제품(브랜드) 색 변경. null 또는 빈 값이면 설정을 지워 앱 기본 색으로 되돌린다.
     */
    @Transactional
    fun updateTheme(request: ThemeSettingDto): ThemeSettingDto {
        val primary = request.primary?.trim().orEmpty()
        if (primary.isEmpty()) {
            appSettingRepository.deleteById(THEME_PRIMARY)
            return ThemeSettingDto()
        }
        require(HEX_COLOR.matches(primary)) { "색상은 #RRGGBB 형식이어야 합니다." }

        val value = primary.uppercase()
        val setting = appSettingRepository.findById(THEME_PRIMARY).orElse(null)
        if (setting == null) {
            appSettingRepository.save(AppSetting(THEME_PRIMARY, value))
        } else {
            setting.settingValue = value
            setting.updateDts = LocalDateTime.now()
        }
        return ThemeSettingDto(primary = value)
    }
}
