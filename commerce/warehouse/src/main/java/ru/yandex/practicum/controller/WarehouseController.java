package ru.yandex.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.dto.shoppingCart.ShoppingCartDto;
import ru.yandex.practicum.dto.warehouse.AddProductToWarehouseRequest;
import ru.yandex.practicum.dto.warehouse.AddressDto;
import ru.yandex.practicum.dto.warehouse.AssemblyProductsForOrderRequest;
import ru.yandex.practicum.dto.warehouse.BookedProductsDto;
import ru.yandex.practicum.dto.warehouse.NewProductInWarehouseRequest;
import ru.yandex.practicum.dto.warehouse.ShippedToDeliveryRequest;
import ru.yandex.practicum.feign.WarehouseOperations;
import ru.yandex.practicum.service.WarehouseService;

import java.util.Map;
import java.util.UUID;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/warehouse")
public class WarehouseController implements WarehouseOperations {
    private final WarehouseService warehouseService;

    @Override
    public void addProduct(NewProductInWarehouseRequest request) {
        log.info("PUT add product to warehouse: {}", request);
        warehouseService.addNewProduct(request);
    }

    @Override
    public BookedProductsDto checkWarehouse(ShoppingCartDto cartDto) {
        log.info("POST check warehouse for cart: {}", cartDto.getShoppingCartId());
        return warehouseService.checkWarehouse(cartDto);
    }

    @Override
    public void increaseProductQuantity(AddProductToWarehouseRequest request) {
        log.info("POST increase product quantity: {}", request);
        warehouseService.increaseProductQuantity(request);
    }

    @Override
    public AddressDto getWarehouseAddress() {
        log.info("GET warehouse address");
        return warehouseService.getAddress();
    }

    @Override
    public void shippedToDelivery(ShippedToDeliveryRequest request) {
        log.info("POST shipped to delivery {}", request);
        warehouseService.shippedToDelivery(request);
    }

    @Override
    public void acceptReturn(Map<UUID, Long> products) {
        log.info("POST accept return {}", products);
        warehouseService.acceptReturn(products);
    }

    @Override
    public BookedProductsDto assemblyProductsForOrder(AssemblyProductsForOrderRequest request) {
        log.info("POST assembly products for orders {}", request.getProducts());
        return warehouseService.assemblyProductsForOrder(request);
    }
}