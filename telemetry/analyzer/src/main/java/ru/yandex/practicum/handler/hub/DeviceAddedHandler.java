package ru.yandex.practicum.handler.hub;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.model.Sensor;
import ru.yandex.practicum.repository.SensorRepository;

@Component
@RequiredArgsConstructor
public class DeviceAddedHandler implements HubEventHandler {
    private final SensorRepository sensorRepository;

    @Override
    public String getType() {
        return DeviceAddedEventAvro.class.getSimpleName();
    }

    @Override
    @Transactional
    public void handle(HubEventAvro event) {
        sensorRepository.save(toSensor(event));
    }

    private Sensor toSensor(HubEventAvro event) {
        DeviceAddedEventAvro deviceAdded = (DeviceAddedEventAvro) event.getPayload();

        return Sensor.builder()
                .id(deviceAdded.getId())
                .hubId(event.getHubId())
                .build();
    }
}