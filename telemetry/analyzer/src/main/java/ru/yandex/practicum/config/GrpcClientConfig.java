package ru.yandex.practicum.config;

import net.devh.boot.grpc.client.inject.GrpcClient;
import net.devh.boot.grpc.client.inject.GrpcClientBean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;

@Configuration
@GrpcClientBean(
        clazz = HubRouterControllerGrpc.HubRouterControllerBlockingStub.class,
        beanName = "hubRouterBlockingStub",
        client = @GrpcClient("hub-router")
)
public class GrpcClientConfig {}