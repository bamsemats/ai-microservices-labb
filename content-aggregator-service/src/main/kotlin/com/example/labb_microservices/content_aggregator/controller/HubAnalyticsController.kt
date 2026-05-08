package com.example.labb_microservices.content_aggregator.controller

import com.example.labb_microservices.content_aggregator.service.HubAnalyticsService
import com.example.labb_microservices.content_aggregator.service.TrendingChannel
import com.example.labb_microservices.content_aggregator.service.UserStats
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.security.Principal

@RestController
@RequestMapping("/analytics")
class HubAnalyticsController(private val analyticsService: HubAnalyticsService) {

    @GetMapping("/trending-channels")
    fun getTrendingChannels(@RequestParam(defaultValue = "10") limit: Long): Flux<TrendingChannel> {
        return analyticsService.getTrendingChannels(limit)
    }

    @GetMapping("/user-stats")
    fun getUserStats(@AuthenticationPrincipal principal: Principal): Mono<UserStats> {
        return analyticsService.getUserStats(principal.name)
    }
}
