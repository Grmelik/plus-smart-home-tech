package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.shoppingStore.ProductCategory;
import ru.yandex.practicum.dto.shoppingStore.ProductDto;
import ru.yandex.practicum.dto.shoppingStore.ProductState;
import ru.yandex.practicum.dto.shoppingStore.QuantityState;
import ru.yandex.practicum.exception.ProductNotFoundException;
import ru.yandex.practicum.mapper.ProductMapper;
import ru.yandex.practicum.model.Product;
import ru.yandex.practicum.repository.ProductRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingStoreServiceImpl implements ShoppingStoreService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDto> getProductsByCategory(ProductCategory category, Pageable pageable) {
        log.debug("Getting products by category: {}, pageable: {}", category, pageable);

        Page<Product> products;
        if (category != null) {
            products = productRepository.findAllByProductCategory(category, pageable);
        } else {
            products = productRepository.findAll(pageable);
        }

        List<ProductDto> productDtoList = products.getContent().stream()
                .map(productMapper::toProductDto)
                .toList();

        return new PageImpl<>(productDtoList, pageable, products.getTotalElements());
    }

    @Override
    @Transactional
    public ProductDto getProductById(UUID productId) {
        log.debug("Getting product by id {}", productId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product with id " + productId + " not found"));
        return productMapper.toProductDto(product);
    }

    @Override
    public ProductDto addProduct(ProductDto productDto) {
        log.debug("Adding new product: {}", productDto);
        Product product = productMapper.toProduct(productDto);
        productRepository.save(product);
        log.info("Product created with id: {}", product.getProductId());
        return productMapper.toProductDto(product);
    }

    @Override
    @Transactional
    public ProductDto updateProduct(ProductDto productDto) {
        log.debug("Updating product: {}", productDto);

        if (productDto.getProductId() == null) {
            throw new ProductNotFoundException("Product not found");
        }

        Product product = productRepository.save(productMapper.toProduct(productDto));
        log.info("Product with id {} updated", product.getProductId());
        return productMapper.toProductDto(product);
    }

    @Override
    @Transactional
    public boolean updateQuantityState(UUID productId, QuantityState quantityState) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product with id " + productId + " not found"));
        product.setQuantityState(quantityState);
        productRepository.save(product);
        log.info("Quantity state updated for product with id {}", product.getProductId());
        return true;
    }

    @Override
    @Transactional
    public boolean removeProduct(UUID productId) {
        log.debug("Removing product with id {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product with id " + productId + "not found"));

        product.setProductState(ProductState.DEACTIVATE);
        productRepository.save(product);
        log.info("Product with id {} deactivated", product.getProductId());
        return true;
    }
}