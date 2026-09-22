package ru.cs.pers_data_masker.store;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.MaskFragment;

import java.time.Duration;
import java.util.List;

/**
 * In-memory реализация {@link CorrelationStore} на Caffeine.
 *
 * <p>TTL 10 минут + ограничение размера. Потокобезопасна.
 */
@Component
public class CaffeineCorrelationStore implements CorrelationStore {

    private static final Duration TTL = Duration.ofMinutes(10);
    private static final long MAX_SIZE = 100_000;

    private final Cache<String, List<MaskFragment>> cache = Caffeine.newBuilder()
            .expireAfterWrite(TTL)
            .maximumSize(MAX_SIZE)
            .build();

    @Override
    public void put(String payloadId, List<MaskFragment> fragments) {
        cache.put(payloadId, fragments);
    }

    @Override
    public List<MaskFragment> get(String payloadId) {
        return cache.getIfPresent(payloadId);
    }

    @Override
    public void remove(String payloadId) {
        cache.invalidate(payloadId);
    }
}