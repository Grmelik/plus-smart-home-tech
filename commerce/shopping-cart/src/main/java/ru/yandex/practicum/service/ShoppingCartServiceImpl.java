package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.shoppingCart.ChangeProductQuantityRequest;
import ru.yandex.practicum.dto.shoppingCart.ShoppingCartDto;
import ru.yandex.practicum.dto.warehouse.BookedProductsDto;
import ru.yandex.practicum.exception.DeactivateCartException;
import ru.yandex.practicum.exception.NoProductsInShoppingCartException;
import ru.yandex.practicum.exception.NotAuthorizedUserException;
import ru.yandex.practicum.feign.WarehouseOperations;
import ru.yandex.practicum.mapper.CartMapper;
import ru.yandex.practicum.model.ShoppingCart;
import ru.yandex.practicum.repository.CartRepository;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShoppingCartServiceImpl implements ShoppingCartService {
    private final CartRepository cartRepository;
    private final CartMapper cartMapper;
    private final WarehouseOperations warehouseClient;

    @Transactional
    @Override
    public ShoppingCartDto getShoppingCart(String username) {
        validateUser(username);
        log.debug("Retrieving shopping cart for the user {}.", username);

        ShoppingCart shoppingCart = getOrCreateShoppingCart(username);
        return cartMapper.toCartDto(shoppingCart);
    }

    @Transactional
    @Override
    public ShoppingCartDto addProductToShoppingCart(String username, Map<UUID, Long> products) {
        validateUser(username);
        log.debug("Adding products {} to the shopping cart for the user {}.", products, username);

        ShoppingCart shoppingCart = getOrCreateShoppingCart(username);
        checkCartIsActive(shoppingCart);

        products.forEach((key, value) -> shoppingCart.getProducts().merge(key, value, Long::sum));
        log.debug("Shopping cart after adding products: {}", shoppingCart);

        BookedProductsDto bookedProductsDto = warehouseClient.checkWarehouse(cartMapper.toCartDto(shoppingCart));
        log.debug("Checked the availability of products {} in the warehouse", bookedProductsDto);

        cartRepository.save(shoppingCart);
        return cartMapper.toCartDto(shoppingCart);
    }

    @Transactional
    @Override
    public void deactivateShoppingCart(String username) {
        validateUser(username);
        log.debug("Deactivating shopping cart for user {}.", username);

        ShoppingCart shoppingCart = getOrCreateShoppingCart(username);
        checkCartIsActive(shoppingCart);
        shoppingCart.setActive(false);
        cartRepository.save(shoppingCart);
        log.debug("Shop[ing cart with id {} was successfully deactivated for username {}.",
                shoppingCart.getCartId(), shoppingCart.getUsername());
    }

    @Transactional
    @Override
    public ShoppingCartDto removeFromShoppingCart(String username, Set<UUID> products) {
        validateUser(username);
        log.debug("Removing products with id: {} from the shopping cart of user {}.", products, username);

        ShoppingCart shoppingCart = getOrCreateShoppingCart(username);
        checkCartIsActive(shoppingCart);
        shoppingCart.getProducts().keySet().removeAll(products);
        ShoppingCart updatedCart = cartRepository.save(shoppingCart);
        log.debug("Updated cart: {}.", updatedCart);

        return cartMapper.toCartDto(updatedCart);
    }

    @Transactional
    @Override
    public ShoppingCartDto changeProductQuantity(String username, ChangeProductQuantityRequest request) {
        log.debug("Changing quantity of the product {} to {} by user {}.", request.getProductId(),
                request.getNewQuantity(), username);
        validateUser(username);
        ShoppingCart shoppingCart = getOrCreateShoppingCart(username);
        checkCartIsActive(shoppingCart);

        if (!shoppingCart.getProducts().containsKey(request.getProductId())) {
            throw new NoProductsInShoppingCartException("No such product in the cart - " + request.getProductId());
        }

        shoppingCart.getProducts().put(request.getProductId(), request.getNewQuantity());
        ShoppingCart updatedCart = cartRepository.save(shoppingCart);
        log.debug("Updated quantity for the product {} in the cart: {} .", request.getProductId(), updatedCart);
        return cartMapper.toCartDto(updatedCart);
    }

    private void validateUser(final String username) {
        log.debug("Validating username {}.", username);
        if (username == null || username.isBlank()) {
            throw new NotAuthorizedUserException("Validation username " + username + " failed.");
        }
    }

    private ShoppingCart getOrCreateShoppingCart(final String username) {
        log.debug("Retrieving shopping cart if exist from DB for username {}.", username);
        return cartRepository.findByUsername(username)
                .orElseGet(() -> {
                    ShoppingCart shoppingCart = new ShoppingCart();
                    shoppingCart.setUsername(username);
                    return cartRepository.save(shoppingCart);
                });
    }

    private void checkCartIsActive(ShoppingCart shoppingCart) {
        if (!shoppingCart.getActive()) {
            throw new DeactivateCartException("The shopping cart is not active");
        }
    }
}