package ru.yandex.practicum.kafka;

import lombok.Getter;

@Getter
public enum KafkaTopic {
    SENSORS("sensors"),
    HUBS("hubs");

    private final String topicKey;

    KafkaTopic(String topicKey) {
        this.topicKey = topicKey;
    }

    public String getTopicName(KafkaConfig config) {
        return config.getTopic(this.topicKey);
    }
}
