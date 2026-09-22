package ru.cs.pers_data_masker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.cs.pers_data_masker.config.SystemConfig;
import ru.cs.pers_data_masker.config.SystemConfigResolver;
import ru.cs.pers_data_masker.detector.DetectorOrchestrator;
import ru.cs.pers_data_masker.domain.MaskFragment;
import ru.cs.pers_data_masker.domain.MaskingResult;
import ru.cs.pers_data_masker.domain.Span;
import ru.cs.pers_data_masker.masking.Demasker;
import ru.cs.pers_data_masker.masking.Masker;
import ru.cs.pers_data_masker.observability.MetricsService;
import ru.cs.pers_data_masker.store.CorrelationStore;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Оркестрирует обработку запроса {@code POST /process}.
 *
 * <p>Направление определяется наличием {@code payload_id} в сторе:
 * <ul>
 *   <li>id отсутствует → маскирование;</li>
 *   <li>id присутствует → демаскирование.</li>
 * </ul>
 *
 * <p>Идемпотентность по {@code payload_id}: повторный запрос возвращает тот же
 * результат из стора (безопасно к ретраям).
 */
@Service
public class ProcessService {

    private static final Logger log = LoggerFactory.getLogger(ProcessService.class);

    private final DetectorOrchestrator orchestrator;
    private final Masker masker;
    private final Demasker demasker;
    private final CorrelationStore store;
    private final SystemConfigResolver systemResolver;
    private final BackpressureGuard backpressureGuard;
    private final MetricsService metrics;

    public ProcessService(DetectorOrchestrator orchestrator,
                          Masker masker,
                          Demasker demasker,
                          CorrelationStore store,
                          SystemConfigResolver systemResolver,
                          BackpressureGuard backpressureGuard,
                          MetricsService metrics) {
        this.orchestrator = orchestrator;
        this.masker = masker;
        this.demasker = demasker;
        this.store = store;
        this.systemResolver = systemResolver;
        this.backpressureGuard = backpressureGuard;
        this.metrics = metrics;
    }

    /**
     * Обрабатывает запрос.
     *
     * @param systemId идентифицированная система
     * @param payload  строка для обработки
     * @param payloadId идентификатор пары
     * @return результат обработки
     */
    public String process(String systemId, String payload, String payloadId) {
        long start = System.nanoTime();
        try (AutoCloseable ignored = backpressureGuard.acquire()) {
            String result = doProcess(systemId, payload, payloadId);
            metrics.recordRequest(start);
            return result;
        } catch (TooManyRequestsException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String doProcess(String systemId, String payload, String payloadId) {
        SystemConfig config = systemResolver.resolve(systemId);
        List<MaskFragment> existing = store.get(payloadId);

        if (existing == null) {
            return mask(payload, payloadId, config);
        }
        if (!config.demasking()) {
            throw new DemaskingNotAllowedException(systemId);
        }
        return demask(payload, existing);
    }

    private String mask(String payload, String payloadId, SystemConfig config) {
        Set<String> allowedTypes = new HashSet<>(config.types());
        List<Span> spans = orchestrator.detect(payload, allowedTypes);
        metrics.recordDetected(spans);
        MaskingResult result = masker.mask(payload, spans);
        store.put(payloadId, result.fragments());
        log.info("Masked payload_id={} types={} spans={}", payloadId, countByType(spans), spans.size());
        return result.maskedText();
    }

    private String demask(String payload, List<MaskFragment> fragments) {
        return demasker.demask(payload, fragments);
    }

    private static java.util.Map<String, Long> countByType(List<Span> spans) {
        java.util.Map<String, Long> counts = new java.util.LinkedHashMap<>();
        for (Span s : spans) {
            counts.merge(s.type(), 1L, Long::sum);
        }
        return counts;
    }
}