package com.example.labb_microservices.message_service.client

import com.example.labb_microservices.proto.UserLookupRequest
import com.example.labb_microservices.proto.UserResponse
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

    fun getUser(userId: String): Mono<UserResponse> {
        val request = UserLookupRequest.newBuilder()
            .setUserId(userId)
            .build()

        val future = userServiceStub
            .withDeadlineAfter(2, TimeUnit.SECONDS)
            .getUserById(request)

        return Mono.create { sink ->
            Futures.addCallback(future, object : FutureCallback<UserResponse> {
                override fun onSuccess(result: UserResponse?) {
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
