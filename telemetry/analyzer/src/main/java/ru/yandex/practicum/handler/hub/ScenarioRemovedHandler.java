package ru.yandex.practicum.handler.hub;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioRemovedEventAvro;
import ru.yandex.practicum.model.Scenario;
import ru.yandex.practicum.repository.ActionRepository;
import ru.yandex.practicum.repository.ConditionRepository;
import ru.yandex.practicum.repository.ScenarioRepository;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScenarioRemovedHandler implements HubEventHandler {
    private final ActionRepository actionRepository;
    private final ConditionRepository conditionRepository;
    private final ScenarioRepository scenarioRepository;

    @Override
    public String getType() {
        return ScenarioRemovedEventAvro.class.getSimpleName();
    }

    @Override
    @Transactional
    public void handle(HubEventAvro event) {
        ScenarioRemovedEventAvro scenarioRemovedAvro = (ScenarioRemovedEventAvro) event.getPayload();
        Optional<Scenario> scenario = scenarioRepository.findByHubIdAndName(event.getHubId(),
                scenarioRemovedAvro.getName());

        if (scenario.isPresent()) {
            Scenario scenarioEntity = scenario.get();
            actionRepository.deleteByScenario(scenarioEntity);
            conditionRepository.deleteByScenario(scenarioEntity);
            scenarioRepository.delete(scenarioEntity);
        } else {
            log.warn("Scenario {} not found.", scenarioRemovedAvro.getName());
        }
    }
}