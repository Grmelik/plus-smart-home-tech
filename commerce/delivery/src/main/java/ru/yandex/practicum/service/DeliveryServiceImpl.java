package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.delivery.DeliveryDto;
import ru.yandex.practicum.dto.delivery.DeliveryState;
import ru.yandex.practicum.dto.order.OrderDto;
import ru.yandex.practicum.dto.warehouse.ShippedToDeliveryRequest;
import ru.yandex.practicum.exception.DeliveryAlreadyExists;
import ru.yandex.practicum.exception.NoDeliveryFoundException;
import ru.yandex.practicum.feign.OrderOperations;
import ru.yandex.practicum.feign.WarehouseOperations;
import ru.yandex.practicum.mapper.DeliveryMapper;
import ru.yandex.practicum.model.Delivery;
import ru.yandex.practicum.repository.DeliveryRepository;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {
    private final DeliveryCalculator deliveryCalculator;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryMapper deliveryMapper;
    private final OrderOperations orderClient;
    private final WarehouseOperations warehouseClient;

    @Transactional
    @Override
    public DeliveryDto planDelivery(DeliveryDto deliveryDto) {
        if (deliveryRepository.existsByOrderId(deliveryDto.getOrderId())) {
            throw new DeliveryAlreadyExists("There is already a delivery order with id=" + deliveryDto.getOrderId());
        }

        log.debug("Create a new delivery: {}", deliveryDto);
        Delivery delivery = deliveryMapper.toDelivery(deliveryDto);
        delivery = deliveryRepository.save(delivery);
        return deliveryMapper.toDeliveryDto(delivery);
    }

    @Transactional
    @Override
    public BigDecimal getDeliveryCost(OrderDto orderDto) {
        Delivery delivery = deliveryRepository.findByOrderId(orderDto.getDeliveryId())
                .orElseThrow(() -> new NoDeliveryFoundException("Delivery for order with id={} not found" +
                        orderDto.getDeliveryId()));
        log.debug("Calculate the cost delivery for the order with id={} ", orderDto.getDeliveryId());
        return deliveryCalculator.calculate(orderDto, delivery);
    }

    @Transactional
    @Override
    public void deliveryPicked(UUID orderId) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoDeliveryFoundException("Delivery for order with id={} not found" + orderId));
        delivery.setDeliveryState(DeliveryState.IN_PROGRESS);
        orderClient.executeAssembly(delivery.getOrderId());
        warehouseClient.shippedToDelivery(new ShippedToDeliveryRequest(orderId, delivery.getDeliveryId()));
        log.debug("The order with id={} has been transferred for delivery", orderId);
        deliveryRepository.save(delivery);
    }

    @Transactional
    @Override
    public void confirmDelivery(UUID orderId) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoDeliveryFoundException("Delivery for order with id={} not found" + orderId));
        delivery.setDeliveryState(DeliveryState.DELIVERED);
        orderClient.makeDelivery(delivery.getOrderId());
        deliveryRepository.save(delivery);
        log.debug("The order delivery confirmed");
    }

    @Transactional
    @Override
    public void deliveryFailed(UUID orderId) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoDeliveryFoundException("Delivery for order with id={} not found" + orderId));
        delivery.setDeliveryState(DeliveryState.FAILED);
        orderClient.deliveryFailed(delivery.getOrderId());
        deliveryRepository.save(delivery);
        log.debug("The order delivery failed");
    }
}