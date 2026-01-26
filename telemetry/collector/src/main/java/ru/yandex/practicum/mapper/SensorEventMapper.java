package ru.yandex.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.LightSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro;
import ru.yandex.practicum.model.ClimateSensorEvent;
import ru.yandex.practicum.model.LightSensorEvent;
import ru.yandex.practicum.model.MotionSensorEvent;
import ru.yandex.practicum.model.SensorEvent;
import ru.yandex.practicum.model.SwitchSensorEvent;
import ru.yandex.practicum.model.TemperatureSensorEvent;

@Component
public class SensorEventMapper {
    public SensorEventAvro toAvro(SensorEvent event) {
        SensorEventAvro.Builder builder = SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp());

        switch (event.getType()) {
            case CLIMATE_SENSOR_EVENT -> {
                ClimateSensorEvent sensorEvent = (ClimateSensorEvent) event;
                builder.setPayload(
                        ClimateSensorAvro.newBuilder()
                                .setTemperatureC(sensorEvent.getTemperatureC())
                                .setHumidity(sensorEvent.getHumidity())
                                .setCo2Level(sensorEvent.getCo2Level())
                                .build()
                );
            }
            case LIGHT_SENSOR_EVENT -> {
                LightSensorEvent sensorEvent = (LightSensorEvent) event;
                builder.setPayload(
                        LightSensorAvro.newBuilder()
                                .setLinkQuality(sensorEvent.getLinkQuality())
                                .setLuminosity(sensorEvent.getLuminosity())
                                .build()
                );
            }
            case MOTION_SENSOR_EVENT -> {
                MotionSensorEvent sensorEvent = (MotionSensorEvent) event;
                builder.setPayload(
                        MotionSensorAvro.newBuilder()
                                .setLinkQuality(sensorEvent.getLinkQuality())
                                .setMotion(sensorEvent.getMotion())
                                .setVoltage(sensorEvent.getVoltage())
                                .build()
                );
            }
            case SWITCH_SENSOR_EVENT -> {
                SwitchSensorEvent sensorEvent = (SwitchSensorEvent) event;
                builder.setPayload(
                        SwitchSensorAvro.newBuilder()
                                .setState(sensorEvent.getState())
                                .build()
                );
            }
            case TEMPERATURE_SENSOR_EVENT -> {
                TemperatureSensorEvent sensorEvent = (TemperatureSensorEvent) event;
                builder.setPayload(
                        TemperatureSensorAvro.newBuilder()
                                .setTemperatureC(sensorEvent.getTemperatureC())
                                .setTemperatureF(sensorEvent.getTemperatureF())
                                .build()
                );
            }
        }
        return builder.build();
    }
}