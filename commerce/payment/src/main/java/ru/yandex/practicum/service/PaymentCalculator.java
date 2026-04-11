package ru.yandex.practicum.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.shoppingStore.ProductDto;
import ru.yandex.practicum.exception.NotEnoughInfoInOrderException;
import ru.yandex.practicum.exception.ProductNotFoundException;
import ru.yandex.practicum.feign.ShoppingStoreOperations;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCalculator {

    private static final BigDecimal FEE_RATE = BigDecimal.valueOf(0.1);
    private static final int SCALE = 2;
    private final ShoppingStoreOperations storeClient;


    public BigDecimal calculateProductCost(final Map<UUID, Long> products) {
        BigDecimal productCost = BigDecimal.valueOf(0.0);
        Set<UUID> ids = products.keySet();
        for (UUID id : ids) {
            BigDecimal price;
            try {
                price = storeClient.getProductById(id).getPrice();
            } catch (FeignException e) {
                if (e.status() == 404) {
                    throw new ProductNotFoundException(e.getMessage());
                } else {
                    throw new RuntimeException(e.getMessage());
                }
            }
            Long quantity = products.get(id);
            productCost = productCost.add(price.multiply(BigDecimal.valueOf(quantity)));
        }
        log.info("Cost of products in the order is {}", productCost);
        return productCost;
    }

    public BigDecimal calculateTotal(final BigDecimal deliveryPrice, final BigDecimal productPrice) {
        BigDecimal feeTotal = productPrice.multiply(BigDecimal.valueOf(0.1));
        BigDecimal totalCost = productPrice.add(feeTotal).add(deliveryPrice);
        log.info("The total cost of the order is {}", totalCost);
        return totalCost;
    }
}