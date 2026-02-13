package ru.yandex.practicum;

import deserializer.SensorEventDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import serializer.GeneralAvroSerializer;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

@Slf4j
@Component
public class AggregationStarter {
    private final KafkaConsumer<String, SensorEventAvro> consumer;
    private final KafkaProducer<String, SensorsSnapshotAvro> producer;
    private final SnapshotStorage snapshotStorage;
    private String inputTopic;
    private String outputTopic;

    public AggregationStarter(
            @Value("${aggregator.kafka.bootstrap-server}") String bootstrapServer,
            @Value("${aggregator.kafka.group-id}") String groupId,
            @Value("${aggregator.kafka.input-topic}") String inputTopic,
            @Value("${aggregator.kafka.output-topic}") String outputTopic,
            SnapshotStorage snapshotStorage
    ) {
        this.inputTopic = inputTopic;
        this.outputTopic = outputTopic;
        this.snapshotStorage = snapshotStorage;

        Properties consumerConfig = new Properties();
        consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        consumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        consumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, SensorEventDeserializer.class.getName());
        consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerConfig.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        consumerConfig.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "500");
        consumerConfig.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "300000");
        consumerConfig.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, "false");

        this.consumer = new KafkaConsumer<>(consumerConfig);

        Properties producerConfig = new Properties();
        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringSerializer");
        producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, GeneralAvroSerializer.class.getName());
        producerConfig.put(ProducerConfig.ACKS_CONFIG, "all");
        producerConfig.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        producerConfig.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        producerConfig.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        producerConfig.put(ProducerConfig.LINGER_MS_CONFIG, "5");
        producerConfig.put(ProducerConfig.BATCH_SIZE_CONFIG, "16384");

        this.producer = new KafkaProducer<>(producerConfig);

        log.info("Kafka consumer and producer created for group: {}", groupId);
    }

    public void start() {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
            consumer.subscribe(List.of(inputTopic));
            log.info("Subscribed for the topic: {}", inputTopic);

            while (true) {
                ConsumerRecords<String, SensorEventAvro> records = consumer.poll(Duration.ofMillis(1000));

                if (!records.isEmpty()) {
                    for (ConsumerRecord<String, SensorEventAvro> record : records) {
                        try {
                            processRecord(record);
                        } catch (Exception e) {
                            log.error("Error processing record at offset {}: {}", record.offset(), e.getMessage());
                        }
                    }
                    consumer.commitSync();
                }
            }
        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.error("Error during event processing", e);
        } finally {
            try {
                producer.flush();
                log.info("All data has been sent to Kafka");
                consumer.commitSync();
                log.info("All offsets are fixed");
            } finally {
                log.info("Closing the consumer");
                consumer.close();
                log.info("Closing the producer");
                producer.close();
            }
        }
    }

    private void processRecord(final ConsumerRecord<String, SensorEventAvro> record) {
        log.info("Processing  ConsumerRecord: topic={}, partition={}, offset={}, hubId={}, timestamp={}",
                record.topic(), record.partition(), record.offset(), record.key(), record.timestamp());

        final Optional<SensorsSnapshotAvro> updatedSnapshot = snapshotStorage.updateState(record.value());
        updatedSnapshot.ifPresent(this::sendSnapshotToKafka);
    }

    private void sendSnapshotToKafka(final SensorsSnapshotAvro snapshot) {
        log.info("Sending snapshot to Kafka: hubId={}", snapshot.getHubId());

        final ProducerRecord<String, SensorsSnapshotAvro> record =
                new ProducerRecord<>(outputTopic, snapshot.getHubId(), snapshot);
        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error("Failed to send snapshot to Kafka", exception);
            } else {
                log.info("Snapshot sent successfully to the topic {} at offset {}", metadata.topic(), metadata.offset());
            }
        });
    }
}