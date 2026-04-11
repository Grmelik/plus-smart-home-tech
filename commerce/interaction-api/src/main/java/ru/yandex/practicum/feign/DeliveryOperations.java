package ru.yandex.practicum.feign;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.dto.delivery.DeliveryDto;
import ru.yandex.practicum.dto.order.OrderDto;

import java.math.BigDecimal;
import java.util.UUID;

@FeignClient(name = "delivery", path = "/api/v1/delivery")
public interface DeliveryOperations {

    @PutMapping
    DeliveryDto planDelivery(@RequestBody @Valid DeliveryDto deliveryDto);

    @PostMapping("/cost")
    BigDecimal getDeliveryCost(@RequestBody @Valid OrderDto orderDto);

    @PostMapping("/picked")
    void deliveryPicked(@RequestBody @NotNull UUID orderId);

    @PostMapping("/successful")
    void confirmDelivery(@RequestBody @NotNull UUID orderId);

    @PostMapping("/failed")
    void deliveryFailed(@RequestBody @NotNull UUID orderId);
}