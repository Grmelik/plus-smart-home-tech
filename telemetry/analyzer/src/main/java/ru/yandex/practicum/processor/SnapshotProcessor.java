package ru.yandex.practicum.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.handler.snapshot.SnapshotHandler;
import ru.yandex.practicum.kafka.ConsumerSnapshotService;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotProcessor implements Runnable {
    private final ConsumerSnapshotService snapshotConsumer;
    private final SnapshotHandler snapshotHandler;
    @Value("${kafka.topics.snapshot}")
    String topic;

    @Override
    public void run() {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Shutting down SnapshotProcessor...");
                snapshotConsumer.wakeup();
            }));
            snapshotConsumer.subscribe(List.of(topic));

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    ConsumerRecords<String, SpecificRecordBase> records =
                            snapshotConsumer.poll(Duration.ofMillis(1000));

                    if (!records.isEmpty()) {
                        for (ConsumerRecord<String, SpecificRecordBase> record : records) {
                            SensorsSnapshotAvro sensorsSnapshot = (SensorsSnapshotAvro) record.value();
                            snapshotHandler.handleSnapshot(sensorsSnapshot);
                        }
                        snapshotConsumer.commitSync();
                    }
                } catch (WakeupException e) {
                    log.error("WakeupException received");
                    break;
                } catch (Exception e) {
                    log.error("Error processing snapshot", e);
                }
            }
        } finally {
            try {
                snapshotConsumer.commitSync();
            } catch (Exception e) {
                log.error("Error committing final offsets", e);
            }
        }
    }
}