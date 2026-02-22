package ru.yandex.practicum.kafka;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import serializer.GeneralAvroSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Getter
@Setter
@ConfigurationProperties(prefix = "kafka")
@Configuration
@Slf4j
public class KafkaConfig {

    private String bootstrapServer = "localhost:9092";
    private Map<String, String> topics = new HashMap<>();
    private Map<String, String> producer = new HashMap<>();

    @Bean
    public Producer<String, SpecificRecordBase> kafkaProducer() {
        log.debug("Initializing Kafka producer with bootstrap servers: {}", bootstrapServer);

        Properties config = new Properties();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, GeneralAvroSerializer.class.getName());
        config.put(ProducerConfig.ACKS_CONFIG, producer.getOrDefault("acks", "all"));
        config.put(ProducerConfig.RETRIES_CONFIG, producer.getOrDefault("retries", "3"));
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION,
                producer.getOrDefault("max-in-flight-requests-per-connection", "5"));
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,
                producer.getOrDefault("enable-idempotence", "true"));
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG,
                producer.getOrDefault("compression-type", "snappy"));
        config.put(ProducerConfig.LINGER_MS_CONFIG, producer.getOrDefault("linger-ms", "5"));
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, producer.getOrDefault("batch-size", "16384"));
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG,
                producer.getOrDefault("buffer-memory", "33554432"));

        return new KafkaProducer<>(config);
    }

    public String getTopic(String topicEnum) {
        if (topics == null) {
            log.error("Topics map is null!");
            throw new IllegalStateException("Topics configuration is missing");
        }
        String topicName = topics.get(topicEnum);
        if (topicName == null || topicName.trim().isEmpty()) {
            log.error("Topic '{}' not found. Available: {}", topicEnum, topics);
            throw new IllegalArgumentException("Undefined Kafka topic: " + topicEnum);
        }
        return topicName;
    }

    @PostConstruct
    private void validateConfig() {
        log.info("Validating Kafka configuration. Bootstrap server: {}", bootstrapServer);
        if (bootstrapServer == null) {
            log.warn("bootstrapServer is not configured, using default: localhost:9092");
            bootstrapServer = "localhost:9092";
        }

        if (topics == null) {
            topics = new HashMap<>();
        }
        topics.putIfAbsent("hubs", "telemetry.hubs.v1");
        topics.putIfAbsent("sensors", "telemetry.sensors.v1");

        if (producer == null) {
            producer = new HashMap<>();
        }
    }
}