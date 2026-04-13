package ru.yandex.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.dto.order.OrderDto;
import ru.yandex.practicum.dto.payment.PaymentDto;
import ru.yandex.practicum.feign.PaymentOperations;
import ru.yandex.practicum.service.PaymentService;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/payment")
public class PaymentController implements PaymentOperations {
    private final PaymentService paymentService;

    @Override
    public PaymentDto createPayment(OrderDto orderDto) {
        PaymentDto payment = paymentService.createPayment(orderDto);
        log.info("Payment with id={} created", payment.getPaymentId());
        return payment;
    }

    @Override
    public BigDecimal getProductCost(OrderDto orderDto) {
        BigDecimal productCost = paymentService.calculateProductCost(orderDto);
        return productCost;
    }

    @Override
    public BigDecimal getTotalCost(OrderDto orderDto) {
        BigDecimal totalCost = paymentService.calculateTotalCost(orderDto);
        return totalCost;
    }

    @Override
    public void confirmPayment(UUID paymentId) {
        paymentService.confirmPayment(paymentId);
    }

    @Override
    public void paymentFailed(UUID paymentId) {
        paymentService.paymentFailed(paymentId);
    }
}