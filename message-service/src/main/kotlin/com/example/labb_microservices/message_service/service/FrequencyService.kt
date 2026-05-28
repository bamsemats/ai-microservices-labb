package com.example.labb_microservices.message_service.service

import com.example.labb_microservices.message_service.model.Frequency
import com.example.labb_microservices.message_service.repository.FrequencyRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus

@Service
class FrequencyService(
    private val frequencyRepository: FrequencyRepository,
    private val mongoTemplate: ReactiveMongoTemplate
) {

    fun createFrequency(name: String, description: String?, ownerId: String): Mono<Frequency> {
        val frequency = Frequency(
            name = name,
            description = description,
            ownerId = ownerId,
            members = setOf(ownerId)
        )
        return frequencyRepository.save(frequency)
    }

    fun getJoinedFrequencies(userId: String): Flux<Frequency> {
        return frequencyRepository.findByMembersContaining(userId)
    }

    fun joinFrequency(frequencyId: String, userId: String): Mono<Frequency> {
        val query = Query(Criteria.where("id").`is`(frequencyId))
        val update = Update().addToSet("members", userId)
        return mongoTemplate.findAndModify(
            query, 
            update, 
            FindAndModifyOptions.options().returnNew(true), 
            Frequency::class.java
        ).switchIfEmpty(Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, "Frequency not found")))
    }

    fun leaveFrequency(frequencyId: String, userId: String): Mono<Void> {
        return frequencyRepository.findById(frequencyId)
            .switchIfEmpty(Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, "Frequency not found")))
            .flatMap { freq ->
                if (freq.ownerId == userId) {
                    val nextOwner = freq.members.filter { it != userId }.minOrNull()
                    if (nextOwner != null) {
                        val query = Query(Criteria.where("id").`is`(frequencyId))
                        val update = Update().pull("members", userId).set("ownerId", nextOwner)
                        mongoTemplate.findAndModify(query, update, Frequency::class.java).then()
                    } else {
                        // Last member and owner leaving, delete frequency
                        frequencyRepository.delete(freq)
                    }
                } else {
                    val query = Query(Criteria.where("id").`is`(frequencyId))
                    val update = Update().pull("members", userId)
                    mongoTemplate.findAndModify(query, update, Frequency::class.java).then()
                }
            }
    }

    fun renameFrequency(frequencyId: String, newName: String, userId: String): Mono<Frequency> {
        val query = Query(Criteria.where("id").`is`(frequencyId).and("ownerId").`is`(userId))
        val update = Update().set("name", newName)
        return mongoTemplate.findAndModify(
            query, 
            update, 
            FindAndModifyOptions.options().returnNew(true), 
            Frequency::class.java
        ).switchIfEmpty(Mono.error(ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized or frequency not found")))
    }

    fun inviteMember(frequencyId: String, memberId: String, userId: String): Mono<Frequency> {
        val query = Query(Criteria.where("id").`is`(frequencyId).and("ownerId").`is`(userId))
        val update = Update().addToSet("members", memberId)
        return mongoTemplate.findAndModify(
            query, 
            update, 
            FindAndModifyOptions.options().returnNew(true), 
            Frequency::class.java
        ).switchIfEmpty(Mono.error(ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized or frequency not found")))
    }

    fun kickMember(frequencyId: String, memberId: String, userId: String): Mono<Void> {
        val query = Query(Criteria.where("id").`is`(frequencyId).and("ownerId").`is`(userId))
        val update = Update().pull("members", memberId)
        return mongoTemplate.findAndModify(
            query, 
            update, 
            Frequency::class.java
        )
        .switchIfEmpty(Mono.error(ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized or frequency not found")))
        .then()
    }
    
    fun findById(id: String): Mono<Frequency> = frequencyRepository.findById(id)
}
