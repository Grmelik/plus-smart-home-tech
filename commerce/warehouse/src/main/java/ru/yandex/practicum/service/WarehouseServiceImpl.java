package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.shoppingCart.ShoppingCartDto;
import ru.yandex.practicum.dto.warehouse.AddProductToWarehouseRequest;
import ru.yandex.practicum.dto.warehouse.AddressDto;
import ru.yandex.practicum.dto.warehouse.BookedProductsDto;
import ru.yandex.practicum.dto.warehouse.NewProductInWarehouseRequest;
import ru.yandex.practicum.exception.NoProductsInShoppingCartException;
import ru.yandex.practicum.exception.NoSpecifiedProductInWarehouseException;
import ru.yandex.practicum.exception.ProductInShoppingCartLowQuantityInWarehouse;
import ru.yandex.practicum.exception.SpecifiedProductAlreadyInWarehouseException;
import ru.yandex.practicum.mapper.ProductMapper;
import ru.yandex.practicum.model.Product;
import ru.yandex.practicum.repository.ProductRepository;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final WarehouseAddressService addressService;

    @Transactional
    @Override
    public void addNewProduct(NewProductInWarehouseRequest request) {
        log.debug("Adding new product to the warehouse: {}", request);
        validateNewProduct(request.getProductId());
        Product product = productRepository.save(productMapper.toEntity(request));
        log.info("The product {} added into the warehouse", product);
    }

    @Transactional
    @Override
    public void increaseProductQuantity(AddProductToWarehouseRequest request) {
        log.debug("The product {} has quantity {}", request.getProductId(), request.getQuantity());
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NoSpecifiedProductInWarehouseException(
                        "The product with id " + request.getProductId() + " is out of warehouse"));
        product.setQuantity(product.getQuantity() + request.getQuantity());
        final Product updatedProduct = productRepository.save(product);
        log.info("The product {} has been updated to a quantity: {}", updatedProduct.getProductId(),
                updatedProduct.getQuantity());
    }

    @Transactional
    @Override
    public BookedProductsDto checkWarehouse(ShoppingCartDto cartDto) {
        if (cartDto.getProducts() == null || cartDto.getProducts().isEmpty()) {
            throw new NoProductsInShoppingCartException("The shopping cart is empty");
        } else {
            log.info("The products {} in the shopping cart", cartDto);
        }

        Set<UUID> productsInCart = cartDto.getProducts().keySet();
        Map<UUID, Product> avalaibleProducts = productRepository.findAllById(productsInCart)
                .stream()
                .collect(Collectors.toMap(Product::getProductId, Function.identity()));
        Set<UUID> missingProducts = productsInCart.stream()
                .filter(uuid -> !avalaibleProducts.containsKey(uuid))
                .collect(Collectors.toSet());
        Set<UUID> quantityProducts = avalaibleProducts.entrySet().stream()
                .filter(entry -> entry.getValue().getQuantity() < cartDto.getProducts().get(entry.getKey()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        if (!missingProducts.isEmpty()) {
            log.warn("There are no products {} in warehouse", missingProducts);
            throw new NoSpecifiedProductInWarehouseException("Missing products: " + missingProducts);
        }

        if (!quantityProducts.isEmpty()) {
            log.warn("The following products {} are not available", quantityProducts);
            throw new ProductInShoppingCartLowQuantityInWarehouse("Not enough products in the warehouse");
        }
        log.debug("The warehouse have the necessary products.");

        double totalWeight = 0;
        double totalVolume = 0;
        BookedProductsDto bookedProductsDto = new BookedProductsDto();

        for (Map.Entry<UUID, Long> entry : cartDto.getProducts().entrySet()) {
            UUID id = entry.getKey();
            long quantity = entry.getValue();
            Product product = avalaibleProducts.get(id);
            totalWeight += product.getWeight() * quantity;
            bookedProductsDto.setDeliveryWeight(totalWeight);
            totalVolume += product.getWidth() * product.getHeight() * product.getDepth() * quantity;
            bookedProductsDto.setDeliveryVolume(totalVolume);

            if (product.getFragile()) {
                bookedProductsDto.setFragile(true);
            }
        }

        return bookedProductsDto;
    }

    @Transactional
    @Override
    public AddressDto getAddress() {
        return addressService.getAddress();
    }

    private void validateNewProduct(final UUID productId) {
        if (productRepository.existsById(productId)) {
            log.warn("The product with id: {} already exists", productId);
            throw new SpecifiedProductAlreadyInWarehouseException("The product is already in the warehouse");
        }
    }
}