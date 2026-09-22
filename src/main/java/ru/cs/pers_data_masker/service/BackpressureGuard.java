package ru.cs.pers_data_masker.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;

/**
 * Ограничивает параллелизм обработки запросов (бекпрешер).
 *
 * <p>При превышении лимита параллелизма/очереди бросает
 * {@link TooManyRequestsException} → {@code 429} + {@code Retry-After}.
 */
@Component
public class BackpressureGuard {

    private final Semaphore semaphore;

    public BackpressureGuard() {
        this.semaphore = new Semaphore(256);
    }

    /**
     * Захватывает слот параллелизма.
     *
     * @return {@link AutoCloseable} для освобождения слота
     * @throws TooManyRequestsException если слот недоступен
     */
    public AutoCloseable acquire() {
        if (!semaphore.tryAcquire()) {
            throw new TooManyRequestsException();
        }
        return semaphore::release;
    }
}