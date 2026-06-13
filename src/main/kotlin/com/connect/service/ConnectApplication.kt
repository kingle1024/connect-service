package com.connect.service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling // 날짜 알림 이메일 발송 스케줄러 활성화
class ConnectApplication

fun main(args: Array<String>) {
	runApplication<ConnectApplication>(*args)
}
