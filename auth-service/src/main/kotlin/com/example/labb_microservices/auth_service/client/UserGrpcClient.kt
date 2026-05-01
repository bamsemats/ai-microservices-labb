package com.example.labb_microservices.auth_service.client

import com.example.labb_microservices.proto.CredentialsRequest
import com.example.labb_microservices.proto.CredentialsResponse
import com.example.labb_microservices.proto.UserServiceGrpc
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.stereotype.Service

@Service
class UserGrpcClient {

    @GrpcClient("user-service")
    private lateinit var userServiceStub: UserServiceGrpc.UserServiceBlockingStub

    fun validateCredentials(username: String, password: String): CredentialsResponse {
        val request = CredentialsRequest.newBuilder()
            .setUsername(username)
            .setPassword(password)
            .build()
        return userServiceStub.validateCredentials(request)
    }
}
