package ru.yandex.practicum.handler.snapshot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.LightSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro;
import ru.yandex.practicum.model.Condition;
import ru.yandex.practicum.model.Scenario;
import ru.yandex.practicum.repository.ActionRepository;
import ru.yandex.practicum.repository.ConditionRepository;
import ru.yandex.practicum.repository.ScenarioRepository;
import ru.yandex.practicum.service.HubRouterClient;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class SnapshotHandler {
    private final ActionRepository actionRepository;
    private final ConditionRepository conditionRepository;
    private final ScenarioRepository scenarioRepository;
    private final HubRouterClient hubRouterClient;

    public void handleSnapshot(SensorsSnapshotAvro snapshot) {
        Map<String, SensorStateAvro> sensorState = snapshot.getSensorsState();
        scenarioRepository.findByHubId(snapshot.getHubId()).stream()
                .filter(scenario -> handleScenario(scenario, sensorState))
                .forEach(scenario -> {
                    sendAction(scenario);
                });
    }

    private Boolean handleScenario(Scenario scenario, Map<String, SensorStateAvro> sensorState) {
        List<Condition> conditions = conditionRepository.findAllByScenario(scenario);

        return conditions.stream()
                .noneMatch(condition -> !checkCondition(condition, sensorState));
    }

    private Boolean checkCondition(Condition condition, Map<String, SensorStateAvro> sensorState) {
        SensorStateAvro sensorStateAvro = sensorState.get(condition.getSensor().getId());

        if (sensorStateAvro == null) {
            return false;
        }

        switch (condition.getType()) {
            case SWITCH -> {
                SwitchSensorAvro switchSensor = (SwitchSensorAvro) sensorStateAvro.getData();
                return handleOperation(condition, switchSensor.getState() ? 1 : 0);
            }
            case MOTION -> {
                MotionSensorAvro motionSensor = (MotionSensorAvro) sensorStateAvro.getData();
                return handleOperation(condition, motionSensor.getMotion() ? 1 : 0);
            }
            case TEMPERATURE -> {
                ClimateSensorAvro temperatureSensor = (ClimateSensorAvro) sensorStateAvro.getData();
                return handleOperation(condition, temperatureSensor.getTemperatureC());
            }
            case LUMINOSITY -> {
                LightSensorAvro lightSensor = (LightSensorAvro) sensorStateAvro.getData();
                return handleOperation(condition, lightSensor.getLuminosity());
            }
            case CO2LEVEL -> {
                ClimateSensorAvro climateSensor = (ClimateSensorAvro) sensorStateAvro.getData();
                return handleOperation(condition, climateSensor.getCo2Level());
            }
            case HUMIDITY -> {
                ClimateSensorAvro climateSensor = (ClimateSensorAvro) sensorStateAvro.getData();
                return handleOperation(condition, climateSensor.getHumidity());
            }
            default -> {
                return false;
            }
        }
    }

    private Boolean handleOperation(Condition condition, Integer value) {
        Integer conditionValue = condition.getValue();

        switch (condition.getOperation()) {
            case EQUALS -> {
                return Objects.equals(value, conditionValue);
            }
            case GREATER_THAN -> {
                return value > conditionValue;
            }
            case LOWER_THAN -> {
                return value < conditionValue;
            }
            default -> {
                return false;
            }
        }
    }

    private void sendAction(Scenario scenario) {
        actionRepository.findAllByScenario(scenario).forEach(hubRouterClient::sendRequest);
    }
}