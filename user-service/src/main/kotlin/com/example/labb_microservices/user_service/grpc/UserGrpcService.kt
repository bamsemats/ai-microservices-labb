package com.example.labb_microservices.user_service.grpc

import com.example.labb_microservices.proto.*
import com.example.labb_microservices.user_service.repository.UserRepository
import io.grpc.stub.StreamObserver
import net.devh.boot.grpc.server.service.GrpcService
import org.springframework.security.crypto.password.PasswordEncoder

import com.example.labb_microservices.user_service.service.UserService

@GrpcService
class UserGrpcService(
    private val userService: UserService,
    private val passwordEncoder: PasswordEncoder
) : UserServiceGrpc.UserServiceImplBase() {

    override fun validateCredentials(
        request: CredentialsRequest,
        responseObserver: StreamObserver<CredentialsResponse>
    ) {
        userService.findByUsername(request.username)
            .map { user ->
                if (passwordEncoder.matches(request.password, user.password)) {
                    CredentialsResponse.newBuilder()
                        .setValid(true)
                        .setUserId(user.id ?: "")
                        .setUsername(user.username)
                        .build()
                } else {
                    CredentialsResponse.newBuilder().setValid(false).build()
                }
            }
            .defaultIfEmpty(CredentialsResponse.newBuilder().setValid(false).build())
            .subscribe({ response ->
                responseObserver.onNext(response)
                responseObserver.onCompleted()
            }, { error ->
                responseObserver.onError(error)
            })
    }

    override fun getUserById(
        request: UserLookupRequest,
        responseObserver: StreamObserver<UserResponse>
    ) {
        userService.findById(request.userId)
            .map { user ->
                UserResponse.newBuilder()
                    .setUserId(user.id ?: "")
                    .setUsername(user.username)
                    .setEmail(user.email ?: "")
                    .setEnabled(user.enabled)
                    .build()
            }
            .subscribe({ response ->
                responseObserver.onNext(response)
                responseObserver.onCompleted()
            }, { error ->
                responseObserver.onError(error)
            }, {
                // On complete if empty
                responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("User not found").asException())
            })
    }
}
