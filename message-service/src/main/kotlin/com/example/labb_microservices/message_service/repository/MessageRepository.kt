package com.example.labb_microservices.message_service.repository

import com.example.labb_microservices.message_service.model.Message
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Repository
interface MessageRepository : ReactiveMongoRepository<Message, String> {
    fun findAllBySearchIndicesContaining(hash: String): Flux<Message>
    fun findAllByChannelId(channelId: String): Flux<Message>
    fun findAllByReceiverIdOrSenderId(receiverId: String, senderId: String): Flux<Message>
}
