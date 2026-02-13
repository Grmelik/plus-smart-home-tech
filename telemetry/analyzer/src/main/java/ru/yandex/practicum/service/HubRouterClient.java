package ru.yandex.practicum.service;

import com.google.protobuf.Timestamp;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;
import ru.yandex.practicum.kafka.telemetry.event.ActionTypeAvro;
import ru.yandex.practicum.model.Action;

import java.time.Instant;

@Service
@Slf4j
public class HubRouterClient {
    private final HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouter;

    public HubRouterClient(@GrpcClient("hub-router") HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouter) {
        this.hubRouter = hubRouter;
    }

    public void sendRequest(Action action) {
        try {
            DeviceActionRequest actionRequest = toActionRequest(action);
            hubRouter.handleDeviceAction(actionRequest);
            log.info("Successfully sent gRPC request for action: {}", action.getId());
        } catch (Exception e) {
            log.error("Error occurred while sending request", e);
        }
    }

    private DeviceActionRequest toActionRequest(Action action) {
        return DeviceActionRequest.newBuilder()
                .setHubId(action.getScenario().getHubId())
                .setScenarioName(action.getScenario().getName())
                .setAction(DeviceActionProto.newBuilder()
                        .setSensorId(action.getSensor().getId())
                        .setType(toActionType(action.getType()))
                        .setValue(action.getValue())
                        .build())
                .setTimestamp(setTimestamp())
                .build();
    }

    private ActionTypeProto toActionType(ActionTypeAvro actionType) {
        return switch (actionType) {
            case ACTIVATE -> ActionTypeProto.ACTIVATE;
            case DEACTIVATE -> ActionTypeProto.DEACTIVATE;
            case INVERSE -> ActionTypeProto.INVERSE;
            case SET_VALUE -> ActionTypeProto.SET_VALUE;
        };
    }

    private Timestamp setTimestamp() {
        Instant instant = Instant.now();
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}