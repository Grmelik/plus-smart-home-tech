package ru.yandex.practicum.handler.hub;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Getter
public class HubEventHandlers {
    private final Map<String, HubEventHandler> handlers;

    public HubEventHandlers(Set<HubEventHandler> handlers) {
        this.handlers = handlers.stream()
                .collect(Collectors.toMap(HubEventHandler::getType, h -> h));
    }
}