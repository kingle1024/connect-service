package com.connect.service.comment

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {
    // 클라이언트가 WebSocket 연결을 맺을 엔드포인트를 등록해
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws-chat") // 예를 들어 'ws://localhost:8080/ws-chat' 이런 식으로 연결하게 돼
                .setAllowedOriginPatterns("*") // 모든 Origin 허용! (나중에 배포할 때는 특정 도메인만 허용하는 게 안전해!)
                .withSockJS() // SockJS 폴백 지원! (웹소켓을 지원 안 하는 브라우저에서도 쓸 수 있도록)
    }

    // 메시지 브로커를 구성해줘
    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        // "/topic"으로 시작하는 메시지는 메시지 브로커를 통해 구독자들에게 broadcast돼
        // "/queue"는 특정 유저에게 1대1 메시지 보낼 때 사용하고
        registry.enableSimpleBroker("/topic", "/queue")
        // 클라이언트가 서버로 메시지를 보낼 때 사용할 접두사야 (예: /app/chat.sendMessage)
        registry.setApplicationDestinationPrefixes("/app")
    }
}
