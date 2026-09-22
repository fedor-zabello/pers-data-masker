package ru.cs.pers_data_masker.service;

/**
 * Исключение: сервис перегружен (превышен лимит параллелизма/очереди).
 */
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException() {
        super("Service is overloaded, retry later");
    }
}