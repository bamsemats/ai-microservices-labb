package com.example.labb_microservices.message_service.service

import com.example.labb_microservices.message_service.model.Frequency
import com.example.labb_microservices.message_service.repository.FrequencyRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class FrequencyService(private val frequencyRepository: FrequencyRepository) {

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
        return frequencyRepository.findById(frequencyId)
            .flatMap { freq ->
                frequencyRepository.save(freq.copy(members = freq.members + userId))
            }
    }

    fun leaveFrequency(frequencyId: String, userId: String): Mono<Void> {
        return frequencyRepository.findById(frequencyId)
            .flatMap { freq ->
                if (freq.ownerId == userId) {
                    // If owner leaves, maybe delete? For now just remove
                    frequencyRepository.save(freq.copy(members = freq.members - userId))
                } else {
                    frequencyRepository.save(freq.copy(members = freq.members - userId))
                }
            }
            .then()
    }
    
    fun findById(id: String): Mono<Frequency> = frequencyRepository.findById(id)
}
