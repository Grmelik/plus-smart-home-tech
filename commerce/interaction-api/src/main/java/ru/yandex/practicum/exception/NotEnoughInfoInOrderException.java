package ru.yandex.practicum.exception;

public class NotEnoughInfoInOrderException extends RuntimeException {
    public NotEnoughInfoInOrderException(String message) {
        super(message);
    }
}