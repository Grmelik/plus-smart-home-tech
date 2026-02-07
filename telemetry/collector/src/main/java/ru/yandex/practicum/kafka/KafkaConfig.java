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

import java.util.Map;
import java.util.Properties;

@Getter
@Setter
@ConfigurationProperties(prefix = "collector.kafka")
@Configuration
@Slf4j
public class KafkaConfig {

    private String bootstrapServer;
    private Map<String, String> topics;
    private Map<String, String> producer;

    @Bean
    public Producer<String, SpecificRecordBase> kafkaProducer() {
        log.debug("Initializing Kafka producer with bootstrap servers: {}", bootstrapServer);

        Properties config = new Properties();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, GeneralAvroSerializer.class.getName());
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        config.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);

        return new KafkaProducer<>(config);
    }

    public String getTopic(String topicEnum) {
        String topicName = topics.get(topicEnum);
        if (topicName == null || topicName.trim().isEmpty()) {
            log.error("Topic '{}' not found. Available: {}", topicEnum, topics);
            throw new IllegalArgumentException("Undefined Kafka topic: " + topicEnum);
        }
        return topicName;
    }

    @PostConstruct
    private void validateConfig() {
        topics.putIfAbsent("hub-events", "telemetry.hubs.v1");
        topics.putIfAbsent("sensor-events", "telemetry.sensors.v1");
    }
}