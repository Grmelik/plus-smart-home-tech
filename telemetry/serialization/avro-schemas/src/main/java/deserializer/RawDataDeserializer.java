package deserializer;

import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class RawDataDeserializer implements Deserializer<String> {

    private static final Logger log = LoggerFactory.getLogger(RawDataDeserializer.class);

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public String deserialize(String topic, byte[] data) {
        if (data == null) return null;

        log.info("=== RAW DATA DUMP for topic: {} ===", topic);
        log.info("Data length: {}", data.length);

        // Hex dump
        StringBuilder hex = new StringBuilder();
        StringBuilder ascii = new StringBuilder();
        for (int i = 0; i < Math.min(data.length, 200); i++) {
            hex.append(String.format("%02x ", data[i]));
            ascii.append(data[i] >= 32 && data[i] < 127 ? (char) data[i] : '.');

            if ((i + 1) % 16 == 0) {
                log.info("{}\t{}", hex.toString(), ascii.toString());
                hex = new StringBuilder();
                ascii = new StringBuilder();
            }
        }
        if (hex.length() > 0) {
            log.info("{}\t{}", hex.toString(), ascii.toString());
        }

        // Попытка как строки
        try {
            String asString = new String(data, StandardCharsets.UTF_8);
            log.info("As UTF-8 string (first 500 chars): {}",
                    asString.substring(0, Math.min(500, asString.length())));
        } catch (Exception e) {
            log.info("Not valid UTF-8");
        }

        return "DUMP_ONLY";
    }

    @Override
    public void close() {
    }
}