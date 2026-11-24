package com.connect.service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.util.TimeZone

// 날짜 알림 이메일 발송 스케줄러는 별도 배치(connect-batch)로 분리됨
@SpringBootApplication
class ConnectApplication

fun main(args: Array<String>) {
	// 배포 컨테이너는 UTC 로 뜬다. LocalDateTime.now() 로 남기는 채팅·댓글·마감 시각이 한국 시간이 되도록
	// 스프링이 뜨기 전에 JVM 기본 시간대를 서울로 고정한다.
	TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
	runApplication<ConnectApplication>(*args)
}
