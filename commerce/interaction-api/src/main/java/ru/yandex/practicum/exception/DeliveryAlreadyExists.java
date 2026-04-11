package ru.yandex.practicum.exception;

public class DeliveryAlreadyExists extends RuntimeException {
    public DeliveryAlreadyExists(String message) {
        super(message);
    }
}