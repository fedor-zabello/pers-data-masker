package ru.cs.pers_data_masker.pydetect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.cs.pers_data_masker.detector.PiiDetector;
import ru.cs.pers_data_masker.domain.Span;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Детектор ПД, делегирующий детекцию Python-микросервису по gRPC.
 *
 * <p>Реализует {@link PiiDetector}, поэтому автоматически подхватывается
 * {@code DetectorOrchestrator}-ом. Типы ПД из Python маппятся в имена типов
 * Java через {@link PyDetectProperties#getTypeMapping()}. Спаны с типами,
 * отсутствующими в маппинге, отбрасываются.
 *
 * <p>При недоступности Python-сервиса возвращает пустой список (graceful
 * degradation) — запрос продолжается с локальными детекторами.
 */
public class PyPiiDetector implements PiiDetector {

    private static final Logger log = LoggerFactory.getLogger(PyPiiDetector.class);

    private final PyDetectClient client;
    private final Map<String, String> typeMapping;
    private final int priority;

    public PyPiiDetector(PyDetectClient client, PyDetectProperties props, int priority) {
        this.client = client;
        this.typeMapping = props.getTypeMapping();
        this.priority = priority;
    }

    @Override
    public List<Span> detect(String text) {
        List<PyDetectSpan> remote = client.detect(text);
        List<Span> spans = new ArrayList<>();
        for (PyDetectSpan s : remote) {
            String javaType = typeMapping.get(s.type());
            if (javaType == null) {
                log.debug("Skipping unmapped python type {}", s.type());
                continue;
            }
            String original = text.substring(s.start(), s.end());
            spans.add(new Span(s.start(), s.end(), javaType, original));
        }
        return spans;
    }

    @Override
    public String type() {
        return "PYTHON";
    }

    @Override
    public int priority() {
        return priority;
    }
}