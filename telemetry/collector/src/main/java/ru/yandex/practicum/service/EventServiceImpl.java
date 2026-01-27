package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.KafkaClient;
import ru.yandex.practicum.mapper.HubEventMapper;
import ru.yandex.practicum.mapper.SensorEventMapper;
import ru.yandex.practicum.model.HubEvent;
import ru.yandex.practicum.model.SensorEvent;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final KafkaClient kafkaClient;
    private final SensorEventMapper sensorEventMapper;
    private final HubEventMapper hubEventMapper;
    @Value("${collector.kafka.producer.topics.sensors-events}")
    private String sensorsEventsTopic;
    @Value("${collector.kafka.producer.topics.hubs-events}")
    private String hubsEventsTopic;

    @Override
    public void produceSensor(SensorEvent sensorEvent) {
        long timestamp = sensorEvent.getTimestamp().toEpochMilli();
        kafkaClient.send(sensorsEventsTopic, sensorEvent.getHubId(), sensorEventMapper.toAvro(sensorEvent),
                timestamp);
    }

    @Override
    public void produceHub(HubEvent hubEvent) {
        long timestamp = hubEvent.getTimestamp().toEpochMilli();
        kafkaClient.send(hubsEventsTopic, hubEvent.getHubId(), hubEventMapper.toAvro(hubEvent), timestamp);
    }
}