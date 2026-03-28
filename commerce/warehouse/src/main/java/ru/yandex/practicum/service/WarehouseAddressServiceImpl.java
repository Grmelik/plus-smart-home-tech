package ru.yandex.practicum.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.warehouse.AddressDto;

import java.security.SecureRandom;
import java.util.Random;

@Component
@Slf4j
public class WarehouseAddressServiceImpl implements WarehouseAddressService {

    private static final String[] addresses = new String[]{"ADDRESS_1", "ADDRESS_2"};
    private static final String address = addresses[Random.from(new SecureRandom()).nextInt(0, 1)];

    @Override
    public AddressDto getAddress() {
        return AddressDto.builder()
                .country(address)
                .city(address)
                .street(address)
                .house(address)
                .flat(address)
                .build();
    }
}