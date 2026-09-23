package ru.cs.pers_data_masker.pydetect;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Конфигурация gRPC-клиента к Python-микросервису детекции.
 *
 * <p>Параметры задаются в {@code application.yaml} в секции {@code pydetect}.
 * При {@code enabled=false} Python-детектор не используется (работают только
 * встроенные Java-детекторы).
 */
@ConfigurationProperties(prefix = "pydetect")
public class PyDetectProperties {

    /** Включён ли Python-детектор. */
    private boolean enabled = false;

    /** Базовый URL HTTP-сервиса Python. */
    private String url = "http://localhost:8000";

    /** Таймаут одного вызова в миллисекундах. */
    private long timeoutMs = 500;

    /** Маппинг имён типов ПД из Python в имена типов Java. */
    private Map<String, String> typeMapping = new HashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public Map<String, String> getTypeMapping() {
        return typeMapping;
    }

    public void setTypeMapping(Map<String, String> typeMapping) {
        this.typeMapping = typeMapping;
    }
}