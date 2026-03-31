package ru.yandex.practicum.kafka;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.health-check.enabled", havingValue = "true")
public class KafkaHealthChecker {

    private final KafkaConfig kafkaConfig;

    @PostConstruct
    public void checkKafkaConnection() {
        log.info("Checking Kafka connection...");

        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getBootstrapServer());
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 5000);

        try (AdminClient adminClient = AdminClient.create(props)) {
            ListTopicsResult topics = adminClient.listTopics();
            topics.names().get(3, TimeUnit.SECONDS);

            log.info("✅ Kafka connection successful. Broker: {}", kafkaConfig.getBootstrapServer());

        } catch (TimeoutException e) {
            log.error("⏱️  Kafka connection timeout. Broker may be unavailable: {}",
                    kafkaConfig.getBootstrapServer());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("⏸️  Kafka connection check interrupted");
        } catch (ExecutionException e) {
            log.error("❌ Kafka connection failed. Error: {}", e.getCause().getMessage());
        } catch (Exception e) {
            log.error("❌ Unexpected error checking Kafka connection", e);
        }
    }
}