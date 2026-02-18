package ru.yandex.practicum.handler.hub;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.kafka.telemetry.event.DeviceRemovedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.model.Condition;
import ru.yandex.practicum.repository.ConditionRepository;
import ru.yandex.practicum.repository.SensorRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeviceRemovedHandler implements HubEventHandler {
    private final ConditionRepository conditionRepository;
    private final SensorRepository sensorRepository;

    @Override
    public String getType() {
        return DeviceRemovedEventAvro.class.getSimpleName();
    }

    @Transactional
    @Override
    public void handle(HubEventAvro event) {
        try {
            DeviceRemovedEventAvro deviceRemoved = (DeviceRemovedEventAvro) event.getPayload();
            String sensorId = deviceRemoved.getId();
            String hubId = event.getHubId();
            List<Condition> conditions = conditionRepository.findBySensorId(sensorId);

            if (!conditions.isEmpty()) {
                log.info("Found {} conditions for sensor {}, removing them", conditions.size(), sensorId);

                for (Condition condition : conditions) {
                    conditionRepository.delete(condition);
                }
            }

            sensorRepository.deleteByIdAndHubId(sensorId, hubId);
            log.info("Successfully removed device {} from hub {}", sensorId, hubId);
        } catch (Exception e) {
            log.error("Failed to remove device from hub event: {}", event, e);
            throw e;
        }
    }
}