package ru.cs.pers_data_masker.pydetect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * HTTP-клиент к Python-микросервису детекции.
 *
 * <p>Вызывает {@code POST /detect} и возвращает спаны. При недоступности
 * Python-сервиса или превышении таймаута логирует ошибку и возвращает пустой
 * список (graceful degradation) — запрос продолжается с локальными детекторами.
 */
public class PyDetectClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(PyDetectClient.class);

    private final RestClient restClient;
    private final long timeoutMs;

    public PyDetectClient(PyDetectProperties props) {
        this.restClient = RestClient.builder()
                .baseUrl(props.getUrl())
                .build();
        this.timeoutMs = props.getTimeoutMs();
    }

    /**
     * Вызывает Python-сервис и возвращает спаны.
     *
     * @param text исходный текст
     * @return список спанов (пустой при недоступности сервиса)
     */
    public List<PyDetectSpan> detect(String text) {
        try {
            DetectResponse response = restClient.post()
                    .uri("/detect")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", text))
                    .retrieve()
                    .body(DetectResponse.class);
            return response == null ? List.of() : response.spans();
        } catch (RuntimeException e) {
            log.warn("Python detect service unavailable, falling back to local detectors: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public void close() {
        // RestClient не требует явного закрытия.
    }

    /** Тело ответа {@code POST /detect}. */
    public record DetectResponse(List<PyDetectSpan> spans) {
    }
}