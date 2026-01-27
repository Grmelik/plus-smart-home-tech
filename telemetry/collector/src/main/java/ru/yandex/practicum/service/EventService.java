package ru.yandex.practicum.service;

import ru.yandex.practicum.model.HubEvent;
import ru.yandex.practicum.model.SensorEvent;

public interface EventService {
    void produceSensor(SensorEvent sensorEvent);

    void produceHub(HubEvent hubEvent);
}