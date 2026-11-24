package com.connect.service.setting.repository

import com.connect.service.setting.entity.AppSetting
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AppSettingRepository : JpaRepository<AppSetting, String>
