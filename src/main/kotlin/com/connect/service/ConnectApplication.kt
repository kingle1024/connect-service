package com.connect.service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ConnectApplication

fun main(args: Array<String>) {
	runApplication<ConnectApplication>(*args)
}
