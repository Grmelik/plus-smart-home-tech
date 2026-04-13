package ru.yandex.practicum.service;

import ru.yandex.practicum.dto.order.CreateNewOrderRequest;
import ru.yandex.practicum.dto.order.OrderDto;
import ru.yandex.practicum.dto.order.OrderState;
import ru.yandex.practicum.dto.order.ProductReturnRequest;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    List<OrderDto> getOrdersByUser(String username);

    OrderDto createOrder(CreateNewOrderRequest request, String username);

    OrderDto returnOrder(ProductReturnRequest request);

    OrderDto processPaymentSuccess(UUID orderId);

    OrderDto updateOrderState(UUID orderId, OrderState orderState);

    OrderDto calculateDeliveryCost(UUID orderId);

    OrderDto calculateTotalCost(UUID orderId);
}