package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.order.OrderDto;
import ru.yandex.practicum.model.Address;
import ru.yandex.practicum.model.Delivery;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryCalculator {
    private static final BigDecimal BASE_COST = BigDecimal.valueOf(5.0);
    private static final BigDecimal ADDRESS_1_COEF = BigDecimal.valueOf(1.0);
    private static final BigDecimal ADDRESS_2_COEF = BigDecimal.valueOf(2.0);
    private static final BigDecimal FRAGILE_COEF = BigDecimal.valueOf(0.2);
    private static final BigDecimal WEIGHT_COEF = BigDecimal.valueOf(0.3);
    private static final BigDecimal VOLUME_COEF = BigDecimal.valueOf(0.2);
    private static final BigDecimal DISTANCE_COEF = BigDecimal.valueOf(0.2);
    private static final int SCALE = 2;

    public BigDecimal calculate(final OrderDto order, final Delivery delivery) {
        log.debug("Calculating the cost of order delivery");

        BigDecimal deliveryCost = BASE_COST;
        deliveryCost = deliveryCost.add(calculateWarehouseCost(deliveryCost, delivery.getFromAddress()));
        deliveryCost = deliveryCost.add(calculateFragileCost(deliveryCost, order.isFragile()));
        deliveryCost = deliveryCost.add(calculateWeightCost(BigDecimal.valueOf(order.getDeliveryWeight())));
        deliveryCost = deliveryCost.add(calculateVolumeCost(BigDecimal.valueOf(order.getDeliveryVolume())));
        deliveryCost = deliveryCost.add(calculateDistanceCost(deliveryCost, delivery.getFromAddress(),
                delivery.getToAddress()));
        return deliveryCost;
    }

    private BigDecimal calculateWarehouseCost(BigDecimal cost, Address warehouse) {
        Objects.requireNonNull(warehouse);
        BigDecimal coefficient = warehouse.getCountry().contains("ADDRESS_2") ? ADDRESS_2_COEF : ADDRESS_1_COEF;
        return cost.multiply(coefficient).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateFragileCost(BigDecimal cost, boolean isFragile) {
        return isFragile ? cost.multiply(FRAGILE_COEF).setScale(SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }

    private BigDecimal calculateWeightCost(BigDecimal weight) {
        return weight.multiply(WEIGHT_COEF).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateVolumeCost(BigDecimal volume) {
        return volume.multiply(VOLUME_COEF).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateDistanceCost(BigDecimal cost, Address from, Address to) {
        return from.getStreet().equals(to.getStreet())
                ? BigDecimal.ZERO
                : cost.multiply(DISTANCE_COEF).setScale(SCALE, RoundingMode.HALF_UP);
    }
}