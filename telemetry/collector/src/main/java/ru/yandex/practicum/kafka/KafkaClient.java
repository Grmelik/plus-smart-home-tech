package ru.yandex.practicum.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaClient implements AutoCloseable {
    private final KafkaProducer<String, SpecificRecordBase> producer;

    public void send(String topic, String key, SpecificRecordBase recordBase, long timestamp) {
        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(topic, null, timestamp, key, recordBase);

        producer.send(record, (recordMetadata, exception) -> {
            if (exception != null) {
                log.error("Ошибка при отправке в Kafka", exception);
            }
        });
    }

    @Override
    public void close() throws Exception {
        producer.flush();
        producer.close();
    }
}