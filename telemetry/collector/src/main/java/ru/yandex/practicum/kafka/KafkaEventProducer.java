package ru.yandex.practicum.kafka;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@Getter
@Setter
@RequiredArgsConstructor
public class KafkaEventProducer {
    private final Producer<String, SpecificRecordBase> producer;

    public void send(final SpecificRecordBase event, final String key,
                     final Instant timestamp, final String topic) {
        log.info("Sending message to Kafka. Topic: {}, Key: {}", topic, key);

        try {
            final ProducerRecord<String, SpecificRecordBase> record =
                    new ProducerRecord<>(topic, null, timestamp.toEpochMilli(), key, event);

            producer.send(record).get(5, TimeUnit.SECONDS);

            log.info("Message sent successfully to topic: {}", topic);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Message sending was interrupted. Topic: {}, Key: {}", topic, key, e);
            throw new RuntimeException("Message sending interrupted", e);
        } catch (ExecutionException e) {
            log.error("Failed to send message. Topic: {}, Key: {}", topic, key, e.getCause());
            throw new RuntimeException("Failed to send message to Kafka", e.getCause());
        } catch (TimeoutException e) {
            log.error("Timeout while sending message. Topic: {}, Key: {}", topic, key, e);
            throw new RuntimeException("Kafka send timeout", e);
        }
    }

    public void sendAsync(final SpecificRecordBase event, final String key,
                          final Instant timestamp, final String topic) {
        log.debug("Sending message asynchronously. Topic: {}, Key: {}", topic, key);

        final ProducerRecord<String, SpecificRecordBase> record =
                new ProducerRecord<>(topic, null, timestamp.toEpochMilli(), key, event);

        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error("Error sending message to Kafka. Topic: {}, Key: {}, Error: {}",
                        topic, key, exception.getMessage(), exception);
            } else {
                log.debug("Message sent successfully. Topic: {}, Partition: {}, Offset: {}",
                        metadata.topic(), metadata.partition(), metadata.offset());
            }
        });
    }
}
