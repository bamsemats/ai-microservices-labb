package com.example.labb_microservices.message_service.client

import com.example.labb_microservices.proto.UserLookupRequest
import com.example.labb_microservices.proto.UserResponse
import com.example.labb_microservices.proto.UserServiceGrpc
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.stereotype.Service

@Service
class UserGrpcClient {

    @GrpcClient("user-service")
    private lateinit var userServiceStub: UserServiceGrpc.UserServiceBlockingStub

    fun getUser(userId: String): UserResponse {
        val request = UserLookupRequest.newBuilder()
            .setUserId(userId)
            .build()
        return userServiceStub.getUserById(request)
    }
}
