package ru.yandex.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.dto.order.CreateNewOrderRequest;
import ru.yandex.practicum.dto.order.OrderDto;
import ru.yandex.practicum.dto.order.OrderState;
import ru.yandex.practicum.dto.order.ProductReturnRequest;
import ru.yandex.practicum.feign.OrderOperations;
import ru.yandex.practicum.service.OrderService;

import java.util.List;
import java.util.UUID;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/order")
public class OrderController implements OrderOperations {
    private final OrderService orderService;

    @Override
    public List<OrderDto> getUserOrders(String username) {
        log.info("Getting orders of user {}", username);
        List<OrderDto> orders = orderService.getOrdersByUser(username);
        return orders;
    }

    @Override
    public OrderDto createNewOrder(CreateNewOrderRequest request, String username) {
        OrderDto newOrder = orderService.createOrder(request, username);
        log.info("Order with id={} has been created for user {}", newOrder.getOrderId(), username);
        return newOrder;
    }

    @Override
    public OrderDto returnProduct(ProductReturnRequest request) {
        log.info("Returning the order with id={}", request.getOrderId());
        OrderDto order = orderService.returnOrder(request);
        return order;
    }

    @Override
    public OrderDto makePayment(UUID orderId) {
        log.info("Making payment for order with id={}", orderId);
        OrderDto order = orderService.processPaymentSuccess(orderId);
        return order;
    }

    @Override
    public OrderDto paymentFailed(UUID orderId) {
        OrderDto order = orderService.updateOrderState(orderId, OrderState.PAYMENT_FAILED);
        log.info("Payment for order with id={} failed", order.getOrderId());
        return order;
    }

    @Override
    public OrderDto makeDelivery(UUID orderId) {
        log.info("Making delivery for order with id={}", orderId);
        OrderDto order = orderService.updateOrderState(orderId, OrderState.DELIVERED);
        return order;
    }

    @Override
    public OrderDto deliveryFailed(UUID orderId) {
        final OrderDto order = orderService.updateOrderState(orderId, OrderState.DELIVERY_FAILED);
        log.info("Delivery for order with id={} failed", order.getOrderId());
        return order;
    }

    @Override
    public OrderDto complete(UUID orderId) {
        log.info("The order with id={} completed", orderId);
        final OrderDto order = orderService.updateOrderState(orderId, OrderState.COMPLETED);
        return order;
    }

    @Override
    public OrderDto calculateTotalCost(UUID orderId) {
        log.info("Calculating total cost for order with id={}", orderId);
        OrderDto order = orderService.calculateTotalCost(orderId);
        return order;
    }

    @Override
    public OrderDto calculateDeliveryCost(UUID orderId) {
        log.info("Calculating delivery cost for order with id={}", orderId);
        OrderDto order = orderService.calculateDeliveryCost(orderId);
        return order;
    }

    @Override
    public OrderDto executeAssembly(UUID orderId) {
        log.info("Order with id={} assembly", orderId);
        OrderDto order = orderService.updateOrderState(orderId, OrderState.ASSEMBLED);
        return order;
    }

    @Override
    public OrderDto assemblyFailed(UUID orderId) {
        final OrderDto order = orderService.updateOrderState(orderId, OrderState.ASSEMBLY_FAILED);
        log.info("Assembly for order with id={} failed", order.getOrderId());
        return order;
    }
}