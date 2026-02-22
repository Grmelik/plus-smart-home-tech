package ru.yandex.practicum.kafka;

import deserializer.SensorsSnapshotDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

@Service
@Slf4j
public class ConsumerSnapshotService {
    final KafkaConsumer<String, SpecificRecordBase> consumer;

    public ConsumerSnapshotService(
            @Value("${kafka.group-id.snapshot}") String groupId,
            @Value("${kafka.bootstrap-server}") String bootstrapServer
    ) {
        Properties config = new Properties();
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, SensorsSnapshotDeserializer.class.getName());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "10");
        config.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "300000");
        config.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, "false");

        log.info("Creating Kafka Snapshot Consumer for group: {}", groupId);
        this.consumer = new KafkaConsumer<>(config);
    }

    public ConsumerRecords<String, SpecificRecordBase> poll(Duration duration) {
        return consumer.poll(duration);
    }

    public void subscribe(List<String> topics) {
        consumer.subscribe(topics);
        log.info("Snapshot consumer subscribed to topics: {}", topics);
    }

    public void commitSync() {
        consumer.commitSync();
    }

    public void wakeup() {
        consumer.wakeup();
    }
}
