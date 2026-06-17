package com.connect.service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

// 날짜 알림 이메일 발송 스케줄러는 별도 배치(connect-batch)로 분리됨
@SpringBootApplication
class ConnectApplication

fun main(args: Array<String>) {
	runApplication<ConnectApplication>(*args)
}
