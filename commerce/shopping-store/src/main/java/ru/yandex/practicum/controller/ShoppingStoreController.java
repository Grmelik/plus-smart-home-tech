package ru.yandex.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.dto.shoppingStore.ProductCategory;
import ru.yandex.practicum.dto.shoppingStore.ProductDto;
import ru.yandex.practicum.dto.shoppingStore.QuantityState;
import ru.yandex.practicum.feign.ShoppingStoreOperations;
import ru.yandex.practicum.service.ShoppingStoreService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shopping-store")
@RequiredArgsConstructor
@Slf4j
public class ShoppingStoreController implements ShoppingStoreOperations {
    private final ShoppingStoreService shoppingStoreService;

    @Override
    public Page<ProductDto> getProductsByCategory(ProductCategory category, Pageable pageable) {
        log.info("GET products by category: {}", category);
        return shoppingStoreService.getProductsByCategory(category, pageable);
    }

    @Override
    public ProductDto getProductById(UUID productId) {
        log.info("GET product by id: {}", productId);
        return shoppingStoreService.getProductById(productId);
    }

    @Override
    public ProductDto createProduct(ProductDto productDto) {
        log.info("PUT create product: {}", productDto);
        return shoppingStoreService.addProduct(productDto);
    }

    @Override
    public ProductDto updateProduct(ProductDto productDto) {
        log.info("POST update product: {}", productDto);
        return shoppingStoreService.updateProduct(productDto);
    }

    @Override
    public boolean removeProductFromStore(UUID productId) {
        log.info("POST remove product: {}", productId);
        return shoppingStoreService.removeProduct(productId);
    }

    @Override
    public boolean updateQuantityState(UUID productId, QuantityState quantityState) {
        log.info("PUT update product quantity {}: {}", productId, quantityState);
        return shoppingStoreService.updateQuantityState(productId, quantityState);
    }
}