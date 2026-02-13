package ru.yandex.practicum.handler.hub;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.kafka.telemetry.event.DeviceActionAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioConditionAvro;
import ru.yandex.practicum.model.Action;
import ru.yandex.practicum.model.Condition;
import ru.yandex.practicum.model.Scenario;
import ru.yandex.practicum.repository.ActionRepository;
import ru.yandex.practicum.repository.ConditionRepository;
import ru.yandex.practicum.repository.ScenarioRepository;
import ru.yandex.practicum.repository.SensorRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ScenarioAddedHandler implements HubEventHandler {
    private final ActionRepository actionRepository;
    private final ConditionRepository conditionRepository;
    private final ScenarioRepository scenarioRepository;
    private final SensorRepository sensorRepository;

    @Override
    public String getType() {
        return ScenarioAddedEventAvro.class.getSimpleName();
    }

    @Override
    @Transactional
    public void handle(HubEventAvro event) {
        ScenarioAddedEventAvro scenarioAddedAvro = (ScenarioAddedEventAvro) event.getPayload();
        Optional<Scenario> scenario = scenarioRepository.findByHubIdAndName(event.getHubId(),
                scenarioAddedAvro.getName());

        Scenario scenarioEntity = scenario.orElseGet(() -> scenarioRepository.save(toScenario(event)));

        if (checkSensorInActions(scenarioAddedAvro, event.getHubId())) {
            actionRepository.saveAll(toActions(scenarioAddedAvro, scenarioEntity));
        }
        if (checkSensorInConditions(scenarioAddedAvro, event.getHubId())) {
            conditionRepository.saveAll(toConditions(scenarioAddedAvro, scenarioEntity));
        }
    }

    private Scenario toScenario(HubEventAvro event) {
        ScenarioAddedEventAvro scenario = (ScenarioAddedEventAvro) event.getPayload();
        return Scenario.builder()
                .name(scenario.getName())
                .hubId(event.getHubId())
                .build();
    }

    private Set<Action> toActions(ScenarioAddedEventAvro event, Scenario scenario) {
        return event.getActions().stream()
                .map(action -> Action.builder()
                        .sensor(sensorRepository.findById(action.getSensorId()).orElseThrow())
                        .scenario(scenario)
                        .type(action.getType())
                        .value(action.getValue())
                        .build())
                .collect(Collectors.toSet());
    }

    private Set<Condition> toConditions(ScenarioAddedEventAvro event, Scenario scenario) {
        return event.getConditions().stream()
                .map(condition -> Condition.builder()
                        .sensor(sensorRepository.findById(condition.getSensorId()).orElseThrow())
                        .scenario(scenario)
                        .type(condition.getType())
                        .operation(condition.getOperation())
                        .value(setValue(condition.getValue()))
                        .build())
                .collect(Collectors.toSet());
    }

    private Boolean checkSensorInActions(ScenarioAddedEventAvro event, String hubId) {
        List<String> sensorIds = event.getActions().stream()
                .map(DeviceActionAvro::getSensorId)
                .toList();
        return sensorRepository.existsByIdInAndHubId(sensorIds, hubId);
    }

    private Boolean checkSensorInConditions(ScenarioAddedEventAvro event, String hubId) {
        List<String> sensorIds = event.getConditions().stream()
                .map(ScenarioConditionAvro::getSensorId)
                .toList();
        return sensorRepository.existsByIdInAndHubId(sensorIds, hubId);
    }

    private Integer setValue(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        } else {
            return (Boolean) value ? 1 : 0;
        }
    }
}