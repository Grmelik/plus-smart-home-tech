package ru.yandex.practicum.exception;

public class NoBookingFoundException extends RuntimeException {
    public NoBookingFoundException(String message) {
        super(message);
    }
}