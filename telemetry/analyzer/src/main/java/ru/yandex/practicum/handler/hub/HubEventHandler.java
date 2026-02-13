package ru.yandex.practicum.handler.hub;

import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

public interface HubEventHandler {
    String getType();

    void handle(HubEventAvro event);
}