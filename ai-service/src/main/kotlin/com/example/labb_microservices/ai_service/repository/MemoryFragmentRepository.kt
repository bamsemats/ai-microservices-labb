package com.example.labb_microservices.ai_service.repository

import com.example.labb_microservices.ai_service.model.MemoryCategory
import com.example.labb_microservices.ai_service.model.MemoryFragment
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface MemoryFragmentRepository : ReactiveMongoRepository<MemoryFragment, String> {
    fun findByUserId(userId: String): Flux<MemoryFragment>
    fun findByUserIdAndCategoryAndValue(userId: String, category: MemoryCategory, value: String): Mono<MemoryFragment>
}
