package ru.cs.pers_data_masker.detector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.config.CustomPiiTypeRegistry;
import ru.cs.pers_data_masker.domain.PersonProfile;
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

    /** Минимальная уверенность спана, чтобы он был принят (0..1). */
    private static final double CONFIDENCE_THRESHOLD = 0.5;

    private final List<PiiDetector> detectors;
    private final SpanConflictResolver conflictResolver;
    private final PersonProfileBuilder profileBuilder;
    private final Map<String, Integer> priorities;

    public DetectorOrchestrator(List<PiiDetector> detectors, SpanConflictResolver conflictResolver,
                                CustomPiiTypeRegistry customRegistry, PersonProfileBuilder profileBuilder) {
        List<PiiDetector> all = new ArrayList<>(detectors);
        all.addAll(customRegistry.detectors());
        this.detectors = all;
        this.conflictResolver = conflictResolver;
        this.profileBuilder = profileBuilder;
        this.priorities = new HashMap<>();
        for (PiiDetector detector : all) {
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
        all.removeIf(span -> span.confidence() < CONFIDENCE_THRESHOLD);
        List<Span> resolved = conflictResolver.resolve(all, type -> priorities.getOrDefault(type, 0));
        List<PersonProfile> profiles = profileBuilder.build(resolved);
        List<Span> result = new ArrayList<>();
        for (PersonProfile profile : profiles) {
            result.addAll(profile.spans());
        }
        return result;
    }
}