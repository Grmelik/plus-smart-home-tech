package ru.yandex.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.dto.delivery.DeliveryDto;
import ru.yandex.practicum.dto.order.OrderDto;
import ru.yandex.practicum.feign.DeliveryOperations;
import ru.yandex.practicum.service.DeliveryService;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/delivery")
public class DeliveryController implements DeliveryOperations {
    private final DeliveryService deliveryService;

    @Override
    public DeliveryDto planDelivery(DeliveryDto deliveryDto) {
        DeliveryDto dto = deliveryService.planDelivery(deliveryDto);
        log.info("Delivery with id={} created", dto.getDeliveryId());
        return dto;
    }

    @Override
    public BigDecimal getDeliveryCost(OrderDto orderDto) {
        BigDecimal deliveryCost = deliveryService.getDeliveryCost(orderDto);
        log.info("Delivery cost is {}", deliveryCost);
        return deliveryCost;
    }

    @Override
    public void deliveryPicked(UUID orderId) {
        deliveryService.deliveryPicked(orderId);
        log.info("Delivery for order with id={} picked", orderId);
    }

    @Override
    public void confirmDelivery(UUID orderId) {
        deliveryService.confirmDelivery(orderId);
        log.info("Delivery for order with id={} confirmed", orderId);
    }

    @Override
    public void deliveryFailed(UUID orderId) {
        deliveryService.deliveryFailed(orderId);
        log.info("Delivery for order with id={} failed", orderId);
    }
}