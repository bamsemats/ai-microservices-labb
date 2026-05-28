package com.example.labb_microservices.message_service.repository

import com.example.labb_microservices.message_service.model.Message
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import org.springframework.data.mongodb.repository.Query
import reactor.core.publisher.Flux

@Repository
interface MessageRepository : ReactiveMongoRepository<Message, String> {

    companion object {
        private const val S = "\$"
    }

    fun findAllBySearchIndicesContaining(hash: String): Flux<Message>

    @Query("{ 'searchIndices': { '${S}all': ?0 } }")
    fun findAllBySearchIndicesContainingAll(hashes: Collection<String>): Flux<Message>

    @Query("{ 'searchIndices': { '${S}all': ?0 }, '${S}or': [ { 'senderId': ?1 }, { 'receiverId': ?1 }, { 'receiverId': 'all' } ] }")
    fun searchForUser(hashes: Collection<String>, principal: String): Flux<Message>

    fun findAllByChannelId(channelId: String): Flux<Message>
    fun findAllByReceiverIdOrSenderId(receiverId: String, senderId: String): Flux<Message>
    fun findAllBySenderIdAndReceiverId(senderId: String, receiverId: String): Flux<Message>
    fun findAllByReceiverId(receiverId: String): Flux<Message>
}