package ru.yandex.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.dto.shoppingCart.ShoppingCartDto;
import ru.yandex.practicum.dto.warehouse.AddProductToWarehouseRequest;
import ru.yandex.practicum.dto.warehouse.AddressDto;
import ru.yandex.practicum.dto.warehouse.BookedProductsDto;
import ru.yandex.practicum.dto.warehouse.NewProductInWarehouseRequest;
import ru.yandex.practicum.feign.WarehouseOperations;
import ru.yandex.practicum.service.WarehouseService;

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
}