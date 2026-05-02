package com.example.labb_microservices.auth_service.client

import com.example.labb_microservices.proto.CredentialsRequest
import com.example.labb_microservices.proto.CredentialsResponse
import com.example.labb_microservices.proto.UserServiceGrpc
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.concurrent.TimeUnit

@Service
class UserGrpcClient {

    @GrpcClient("user-service")
    private lateinit var userServiceStub: UserServiceGrpc.UserServiceFutureStub

    fun validateCredentials(username: String, password: String): Mono<CredentialsResponse> {
        val request = CredentialsRequest.newBuilder()
            .setUsername(username)
            .setPassword(password)
            .build()

        val future = userServiceStub
            .withDeadlineAfter(2, TimeUnit.SECONDS)
            .validateCredentials(request)

        return Mono.create { sink ->
            Futures.addCallback(future, object : FutureCallback<CredentialsResponse> {
                override fun onSuccess(result: CredentialsResponse?) {
                    if (result != null) {
                        sink.success(result)
                    } else {
                        sink.success()
                    }
                }

                override fun onFailure(t: Throwable) {
                    sink.error(t)
                }
            }, MoreExecutors.directExecutor())
        }
    }
}
