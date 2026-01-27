package ru.yandex.practicum.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class ScenarioCondition {
    @NotBlank
    private String sensorId;
    private ConditionType type;
    private ConditionOperation operation;
    private Integer value;
}