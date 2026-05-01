package com.example.labb_microservices.message_service.config

import com.example.labb_microservices.message_service.handler.MessageWebSocketHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter

@Configuration
class WebSocketConfig(private val messageWebSocketHandler: MessageWebSocketHandler) {

    @Bean
    fun handlerMapping(): HandlerMapping {
        val mapping = mutableMapOf<String, Any>()
        mapping["/ws/messages"] = messageWebSocketHandler
        return SimpleUrlHandlerMapping(mapping, -1)
    }

    @Bean
    fun handlerAdapter(): WebSocketHandlerAdapter {
        return WebSocketHandlerAdapter()
    }
}
