package ru.yandex.practicum.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.RecordDeserializationException;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.handler.hub.HubEventHandler;
import ru.yandex.practicum.handler.hub.HubEventHandlers;
import ru.yandex.practicum.kafka.ConsumerHubService;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubEventProcessor implements Runnable {
    private final ConsumerHubService hubConsumer;
    private final HubEventHandlers hubHandlers;
    @Value("${kafka.topics.hub}")
    String topic;

    @Override
    public void run() {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(hubConsumer::wakeup));
            hubConsumer.subscribe(List.of(topic));
            Map<String, HubEventHandler> hubHandlersMap = hubHandlers.getHandlers();

            while (true) {
                try {
                    ConsumerRecords<String, SpecificRecordBase> records = hubConsumer.poll(Duration.ofMillis(1000));

                    for (ConsumerRecord<String, SpecificRecordBase> record : records) {
                        try {
                            HubEventAvro hubEvent = (HubEventAvro) record.value();
                            String payloadName = hubEvent.getPayload().getClass().getSimpleName();

                            if (hubHandlersMap.containsKey(payloadName)) {
                                hubHandlersMap.get(payloadName).handle(hubEvent);
                            } else {
                                log.warn("No handler for event type: {}", payloadName);
                            }
                        } catch (ClassCastException e) {
                            log.error("Failed to cast record value to HubEventAvro. Topic: {}, Key: {}",
                                    record.topic(), record.key());
                            continue;
                        } catch (Exception e) {
                            log.error("Error processing record. Topic: {}, Offset: {}",
                                    record.topic(), record.offset(), e);
                        }
                    }
                    if (!records.isEmpty()) {
                        hubConsumer.commitSync();
                    }
                } catch (RecordDeserializationException e) {
                    log.error("Deserialization error at offset {}. Seeking to next offset.", e.offset(), e);
                    hubConsumer.seek(e.topicPartition(), e.offset() + 1);
                }
            }
        } catch (WakeupException ignored) {
            log.error("WakeupException received");
        } catch (Exception e) {
            log.error("Error occurred while processing messages", e);
        } finally {
            try {
                hubConsumer.commitSync();
            } catch (Exception e) {
                log.error("Error occurred while flushing data", e);
            }
        }
    }
}