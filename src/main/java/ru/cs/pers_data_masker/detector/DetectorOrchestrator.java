package ru.cs.pers_data_masker.detector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.Span;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Оркестратор детекторов ПД.
 *
 * <p>Собирает все {@link PiiDetector}, фильтрует по набору типов, разрешённых
 * для системы, объединяет результаты и разрешает пересечения спанов.
 *
 * <p>Graceful degradation: ошибка отдельного детектора логируется и пропускает
 * только свой тип, не роняя весь запрос.
 */
@Component
public class DetectorOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(DetectorOrchestrator.class);

    private final List<PiiDetector> detectors;
    private final SpanConflictResolver conflictResolver;
    private final Map<String, Integer> priorities;

    public DetectorOrchestrator(List<PiiDetector> detectors, SpanConflictResolver conflictResolver) {
        this.detectors = detectors;
        this.conflictResolver = conflictResolver;
        this.priorities = new HashMap<>();
        for (PiiDetector detector : detectors) {
            priorities.put(detector.type(), detector.priority());
        }
    }

    /**
     * Находит все ПД в тексте для заданного набора разрешённых типов.
     *
     * @param text          исходный текст
     * @param allowedTypes  набор имён типов, разрешённых для системы
     * @return непересекающиеся спаны
     */
    public List<Span> detect(String text, Set<String> allowedTypes) {
        List<Span> all = new ArrayList<>();
        for (PiiDetector detector : detectors) {
            if (!allowedTypes.contains(detector.type())) {
                continue;
            }
            try {
                all.addAll(detector.detect(text));
            } catch (RuntimeException e) {
                log.warn("Detector {} failed, skipping its type", detector.type(), e);
            }
        }
        return conflictResolver.resolve(all, type -> priorities.getOrDefault(type, 0));
    }
}