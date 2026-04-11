package ru.yandex.practicum.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.delivery.DeliveryDto;
import ru.yandex.practicum.dto.delivery.DeliveryState;
import ru.yandex.practicum.dto.order.CreateNewOrderRequest;
import ru.yandex.practicum.dto.order.OrderDto;
import ru.yandex.practicum.dto.order.OrderState;
import ru.yandex.practicum.dto.order.ProductReturnRequest;
import ru.yandex.practicum.dto.warehouse.AddProductToWarehouseRequest;
import ru.yandex.practicum.dto.warehouse.AssemblyProductsForOrderRequest;
import ru.yandex.practicum.dto.warehouse.BookedProductsDto;
import ru.yandex.practicum.exception.NoOrderFoundException;
import ru.yandex.practicum.exception.NoSpecifiedProductInWarehouseException;
import ru.yandex.practicum.exception.NotAuthorizedUserException;
import ru.yandex.practicum.exception.ProductInShoppingCartLowQuantityInWarehouse;
import ru.yandex.practicum.feign.DeliveryOperations;
import ru.yandex.practicum.feign.PaymentOperations;
import ru.yandex.practicum.feign.ShoppingCartOperations;
import ru.yandex.practicum.feign.WarehouseOperations;
import ru.yandex.practicum.mapper.OrderMapper;
import ru.yandex.practicum.model.Order;
import ru.yandex.practicum.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final DeliveryOperations deliveryClient;
    private final PaymentOperations paymentClient;
    private final ShoppingCartOperations shoppingCartClient;
    private final WarehouseOperations warehouseClient;

    @Transactional(readOnly = true)
    @Override
    public List<OrderDto> getOrdersByUser(String username) {
        validateUser(username);
        log.debug("Getting the orders of user {}", username);
        final List<Order> orders = orderRepository.findAllByUsername(username);
        return orders.stream()
                .map(orderMapper::toOrderDto)
                .toList();
    }

    @Transactional
    @Override
    public OrderDto createOrder(CreateNewOrderRequest request, String username) {
        validateUser(username);
        log.info("Creating new order {} for user {}", request.getShoppingCart().getProducts(), username);
        BookedProductsDto bookedProduct;

        try {
            bookedProduct = warehouseClient.checkWarehouse(request.getShoppingCart());
            log.info("The products {} on the warehouse checked", bookedProduct);
        } catch (FeignException e) {
            if (e.status() == 400) {
                throw new ProductInShoppingCartLowQuantityInWarehouse(e.getMessage());
            } else if (e.status() == 404) {
                throw new NoSpecifiedProductInWarehouseException(e.getMessage());
            } else {
                throw new RuntimeException(e.getMessage());
            }
        }
        Order newOrder = orderRepository.save(orderMapper.toOrder(request, bookedProduct, username));

        DeliveryDto newDelivery = new DeliveryDto();
        newDelivery.setFromAddress(warehouseClient.getWarehouseAddress());
        newDelivery.setToAddress(request.getDeliveryAddress());
        newDelivery.setOrderId(newOrder.getOrderId());
        newDelivery.setDeliveryState(DeliveryState.CREATED);

        DeliveryDto createdDelivery = deliveryClient.planDelivery(newDelivery);

        newOrder.setDeliveryId(createdDelivery.getDeliveryId());
        Order savedOrder = orderRepository.save(newOrder);
        log.info("New order {} saved in the database", savedOrder);
        return orderMapper.toOrderDto(savedOrder);
    }

    @Transactional
    @Override
    public OrderDto returnOrder(ProductReturnRequest request) {
        log.info("A return request has been created for the order with id={}", request.getOrderId());
        Order order = getOrderById(request.getOrderId());
        Map<UUID, Long> stock = request.getProducts();
        Set<UUID> productIds = stock.keySet();

        for (UUID productId : productIds) {
            AddProductToWarehouseRequest stockRequest = new AddProductToWarehouseRequest(productId, stock.get(productId));
            warehouseClient.increaseProductQuantity(stockRequest);
        }
        log.info("The products {} from order with id={} have been returned to the warehouse", request.getProducts(),
                request.getOrderId());

        Order updatedOrder = changeOrderState(order, OrderState.PRODUCT_RETURNED);
        return orderMapper.toOrderDto(updatedOrder);
    }

    @Transactional
    @Override
    public OrderDto processPaymentSuccess(UUID orderId) {
        log.debug("Processing payment for order with id={}", orderId);
        Order order = getOrderById(orderId);
        //order.setState(OrderState.PAID);
        warehouseClient.assemblyProductsForOrder(new AssemblyProductsForOrderRequest(order.getProducts(), orderId));
        //Order savedOrder = orderRepository.save(order);
        Order savedOrder = changeOrderState(order, OrderState.PAID);
        return orderMapper.toOrderDto(savedOrder);
    }

    @Transactional
    @Override
    public OrderDto updateOrderState(UUID orderId, OrderState orderState) {
        Order order = getOrderById(orderId);
        Order updatedOrder = changeOrderState(order, orderState);
        return orderMapper.toOrderDto(updatedOrder);
    }

    @Transactional
    @Override
    public OrderDto calculateDeliveryCost(UUID orderId) {
        Order order = getOrderById(orderId);
        BigDecimal deliveryCost = deliveryClient.getDeliveryCost(orderMapper.toOrderDto(order));
        order.setDeliveryPrice(deliveryCost);
        Order updatedOrder = orderRepository.save(order);
        return orderMapper.toOrderDto(updatedOrder);
    }

    @Transactional
    @Override
    public OrderDto calculateTotalCost(UUID orderId) {
        Order order = getOrderById(orderId);
        BigDecimal totalCost = paymentClient.getTotalCost(orderMapper.toOrderDto(order));
        order.setTotalPrice(totalCost);
        Order updatedOrder = orderRepository.save(order);
        return orderMapper.toOrderDto(updatedOrder);
    }

    private void validateUser(final String username) {
        log.debug("Validating username {}.", username);
        if (username == null || username.isBlank()) {
            throw new NotAuthorizedUserException("User " + username + " validation failed");
        }
    }

    private Order changeOrderState(Order order, OrderState state) {
        order.setState(state);
        return orderRepository.save(order);
    }

    private Order getOrderById(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NoOrderFoundException("Order with id= " + orderId + " not found"));
    }
}