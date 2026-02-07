package ru.yandex.practicum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {
    private final KafkaConsumer<String, SensorEventAvro> consumer;
    private final KafkaProducer<String, SensorsSnapshotAvro> producer;
    private final SnapshotStorage snapshotStorage;
    @Value("${aggregator.kafka.input-topic}")
    private String inputTopic;
    @Value("${aggregator.kafka.output-topic}")
    private String outputTopic;

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