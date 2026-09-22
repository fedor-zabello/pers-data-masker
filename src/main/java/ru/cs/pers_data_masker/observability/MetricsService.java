package ru.cs.pers_data_masker.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.Span;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Кастомные метрики: счётчики найденных ПД по типам, гистограмма latency,
 * счётчик запросов (для TPS).
 */
@Component
public class MetricsService {

    private final MeterRegistry registry;
    private final Counter requests;
    private final Timer latency;

    public MetricsService(MeterRegistry registry) {
        this.registry = registry;
        this.requests = registry.counter("pii.requests");
        this.latency = registry.timer("pii.process.latency");
    }

    /**
     * Регистрирует найденные ПД по типам.
     */
    public void recordDetected(List<Span> spans) {
        for (Span span : spans) {
            Counter.builder("pii.detected")
                    .tag("type", span.type())
                    .register(registry)
                    .increment();
        }
    }

    /**
     * Регистрирует запрос и его latency.
     */
    public void recordRequest(long startNanos) {
        requests.increment();
        latency.record(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
    }
}