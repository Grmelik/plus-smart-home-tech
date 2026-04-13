package ru.yandex.practicum.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.order.OrderDto;
import ru.yandex.practicum.dto.payment.PaymentDto;
import ru.yandex.practicum.dto.payment.PaymentState;
import ru.yandex.practicum.exception.NoOrderFoundException;
import ru.yandex.practicum.exception.NoPaymentFoundException;
import ru.yandex.practicum.exception.NotEnoughInfoInOrderException;
import ru.yandex.practicum.feign.OrderOperations;
import ru.yandex.practicum.mapper.PaymentMapper;
import ru.yandex.practicum.model.Payment;
import ru.yandex.practicum.repository.PaymentRepository;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final OrderOperations orderClient;
    private final PaymentCalculator paymentCalculator;
    private final PaymentMapper paymentMapper;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public PaymentDto createPayment(OrderDto orderDto) {
        log.debug("Create a new payment for the order with id={}", orderDto.getOrderId());

        BigDecimal productCost = orderDto.getProductPrice();
        BigDecimal deliveryTotal = orderDto.getDeliveryPrice();
        BigDecimal totalCost = orderDto.getTotalPrice();

        if (productCost == null || deliveryTotal == null || totalCost == null) {
            throw new NotEnoughInfoInOrderException("Not enough information to calculate payment");
        }
        BigDecimal feeTotal = productCost.multiply(BigDecimal.valueOf(0.1));
        Payment payment = paymentMapper.toPayment(orderDto, feeTotal);
        log.debug("The payment is {}", payment);
        Payment paymentCreated = paymentRepository.save(payment);

        return paymentMapper.toPaymentDto(paymentCreated);
    }

    @Override
    public BigDecimal calculateProductCost(OrderDto orderDto) {
        if (orderDto.getProducts() == null || orderDto.getProducts().isEmpty()) {
            throw new NotEnoughInfoInOrderException("Order with id=" + orderDto.getOrderId());
        }
        return paymentCalculator.calculateProductCost(orderDto.getProducts());
    }

    @Override
    public BigDecimal calculateTotalCost(OrderDto orderDto) {
        if (orderDto.getDeliveryPrice() == null || orderDto.getDeliveryPrice().equals(BigDecimal.ZERO)) {
            throw new NotEnoughInfoInOrderException("Order with id=" + orderDto.getOrderId());
        }

        if (orderDto.getProductPrice() == null || orderDto.getProductPrice().equals(BigDecimal.ZERO)) {
            throw new NotEnoughInfoInOrderException("Order with id=" + orderDto.getOrderId());
        }

        return paymentCalculator.calculateTotal(orderDto.getDeliveryPrice(), orderDto.getProductPrice());
    }

    @Override
    @Transactional
    public void confirmPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NoPaymentFoundException("Payment not found"));
        payment.setPaymentState(PaymentState.SUCCESS);
        paymentRepository.save(payment);

        try {
            orderClient.makePayment(payment.getOrderId());
        } catch (FeignException e) {
            if (e instanceof FeignException.NotFound) {
                throw new NoOrderFoundException(e.getMessage());
            }
        }
        log.debug("The payment with id={} has been completed", paymentId);
    }

    @Override
    @Transactional
    public void paymentFailed(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NoPaymentFoundException("Payment not found"));
        payment.setPaymentState(PaymentState.FAILED);
        paymentRepository.save(payment);

        try {
            orderClient.paymentFailed(payment.getOrderId());
        } catch (FeignException e) {
            if (e instanceof FeignException.NotFound) {
                throw new NoOrderFoundException(e.getMessage());
            }
        }
        log.debug("Payment with id={} failed", paymentId);
    }
}