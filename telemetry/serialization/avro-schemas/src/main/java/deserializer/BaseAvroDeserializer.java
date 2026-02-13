package deserializer;

import exception.DeserializationException;
import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;

public class BaseAvroDeserializer<T extends SpecificRecordBase> implements Deserializer<T> {

    private static final Logger log = LoggerFactory.getLogger(BaseAvroDeserializer.class);
    private final Class<T> targetType;
    private final DecoderFactory decoderFactory = DecoderFactory.get();
    private final Schema schema;

    public BaseAvroDeserializer(Class<T> targetType) {
        this.targetType = targetType;
        try {
            // Получаем схему из класса Avro
            //this.schema = targetType.newInstance().getSchema();
            Method getClassSchemaMethod = targetType.getMethod("getClassSchema");
            this.schema = (Schema) getClassSchemaMethod.invoke(null);
            log.info("Initialized deserializer for Avro type: {}, Schema: {}", targetType.getName(), schema.getName());
        } catch (Exception e) {
            log.error("Failed to get schema for type: {}", targetType.getName(), e);
            throw new RuntimeException("Failed to initialize deserializer", e);
        }
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) {
            log.warn("Received null data for topic {}", topic);
            return null;
        }

        if (data.length == 0) {
            log.warn("Received empty data for topic {}", topic);
            return null;
        }

        ByteArrayInputStream inputStream = null;
        try {
            log.debug("Deserializing {} bytes for topic {}", data.length, topic);

            // Отладочная информация
            if (log.isDebugEnabled()) {
                log.debug("First 32 bytes (hex): {}", bytesToHex(data, 32));

                // Проверка на Avro magic byte (обычно 0xC3 для raw Avro)
                if (data.length > 0) {
                    log.debug("First byte: 0x{}", String.format("%02X", data[0] & 0xFF));
                }
            }

            // Создаем DatumReader с явным указанием writer и reader схем
            DatumReader<T> reader = new SpecificDatumReader<>(schema);

            inputStream = new ByteArrayInputStream(data);
            BinaryDecoder decoder = decoderFactory.binaryDecoder(inputStream, null);

            T result = reader.read(null, decoder);

            log.debug("Successfully deserialized message for topic: {}", topic);
            return result;

        } catch (Exception e) {
            log.error("Failed to deserialize Avro data. Topic: {}, Data length: {}, Error: {}",
                    topic, data.length, e.getMessage());

            // Детальная отладочная информация
            logDetailedDebugInfo(topic, data, e);

            throw new DeserializationException(
                    String.format("Error deserializing data from topic [%s]. Data length: %d",
                            topic, data.length), e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.warn("Failed to close input stream", e);
                }
            }
        }
    }

    private void logDetailedDebugInfo(String topic, byte[] data, Exception e) {
        if (log.isDebugEnabled()) {
            try {
                // Пытаемся прочитать первые байты для диагностики
                log.debug("=== AVRO DESERIALIZATION DEBUG ===");
                log.debug("Topic: {}", topic);
                log.debug("Data length: {}", data.length);
                log.debug("Target type: {}", targetType.getName());

                // Выводим первые 100 байт в hex
                log.debug("First 100 bytes (hex):\n{}", bytesToHex(data, 100));

                // Пытаемся определить тип данных
                if (data.length >= 4) {
                    int potentialSize = ((data[0] & 0xFF) << 24) |
                            ((data[1] & 0xFF) << 16) |
                            ((data[2] & 0xFF) << 8) |
                            (data[3] & 0xFF);
                    log.debug("First 4 bytes as int (possible Avro array/map size): {}", potentialSize);
                }

                // Выводим схему для справки
                log.debug("Expected schema:\n{}", schema.toString(true));

            } catch (Exception ex) {
                log.debug("Failed to generate debug info", ex);
            }
        }
    }

    private static String bytesToHex(byte[] bytes, int maxLength) {
        if (bytes == null) return "null";

        int length = Math.min(bytes.length, maxLength);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (i > 0 && i % 16 == 0) sb.append("\n");
            sb.append(String.format("%02x ", bytes[i] & 0xFF));
        }
        if (bytes.length > maxLength) {
            sb.append("\n... (total ").append(bytes.length).append(" bytes)");
        }
        return sb.toString();
    }

    @Override
    public void close() {
        // Cleanup if needed
    }
}