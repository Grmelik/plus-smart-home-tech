package deserializer;

import exception.DeserializationException;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class BaseAvroDeserializer<T extends SpecificRecordBase> implements Deserializer<T> {

    private static final Logger log = LoggerFactory.getLogger(BaseAvroDeserializer.class);
    private final Class<T> targetType;

    public BaseAvroDeserializer(Class<T> targetType) {
        this.targetType = targetType;
        log.info("Initialized deserializer for Avro type: {}", targetType.getName());
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x ", b));
        }
        return sb.toString();
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) {
            log.warn("Received null data for topic {}", topic);
            return null;
        }

        try {
            log.debug("Attempting to deserialize {} bytes for topic {}", data.length, topic);

            String firstBytes = bytesToHex(Arrays.copyOfRange(data, 0, Math.min(10, data.length)));
            log.debug("First bytes: {}...", firstBytes);

            DatumReader<T> reader = new SpecificDatumReader<>(targetType);
            T result = reader.read(null, DecoderFactory.get().binaryDecoder(data, null));

            log.debug("Successfully deserialized message for topic: {}", topic);
            return result;
        } catch (Exception e) {
            log.error("Failed to deserialize Avro data. Length: {}, Topic: {}, First 10 bytes: {}",
                    data.length, topic,
                    bytesToHex(Arrays.copyOfRange(data, 0, Math.min(10, data.length))),
                    e);
            throw new DeserializationException("Error deserializing data from a topic [" + topic + "]", e);
        }
    }
}