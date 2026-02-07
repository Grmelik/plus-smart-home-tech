package ru.yandex.practicum.kafka;
/*
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro;

import java.time.Instant;

@Slf4j
@Component
public class KafkaTestRunner implements CommandLineRunner {

    private final KafkaEventProducer producer;
    private final KafkaConfig config;

    public KafkaTestRunner(KafkaEventProducer producer, KafkaConfig config) {
        this.producer = producer;
        this.config = config;
    }

    @Override
    public void run(String... args) {
        ClimateSensorAvro event = ClimateSensorAvro.newBuilder()
                .setTemperatureC((int) 22.5)
                .setHumidity((int) 65.0)
                .setCo2Level(450)
                .build();

        try {
            producer.send(
                    event,
                    "test-sensor-1",
                    Instant.now(),
                    config.getTopic("sensors")
            );
            log.info("Test message sent successfully");
        } catch (Exception e) {
            log.error("Failed to send test message", e);
        }
    }
}*/

public class KafkaTestRunner{}