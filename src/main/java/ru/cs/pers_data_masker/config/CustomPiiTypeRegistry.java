package ru.cs.pers_data_masker.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.detector.PiiDetector;
import ru.cs.pers_data_masker.detector.RegexPiiDetector;
import ru.cs.pers_data_masker.masking.FlexibleMaskingStrategy;
import ru.cs.pers_data_masker.masking.MaskingStrategy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Реестр кастомных типов ПД из YAML.
 *
 * <p>Валидирует конфигурацию при старте (компиляция regex, уникальность имён,
 * fail-fast) и предоставляет доступ к детекторам и стратегиям по имени типа.
 */
@Component
public class CustomPiiTypeRegistry {

    private static final Logger log = LoggerFactory.getLogger(CustomPiiTypeRegistry.class);

    private final Map<String, CustomPiiTypeConfig> byName = new LinkedHashMap<>();
    private final Map<String, PiiDetector> detectors = new LinkedHashMap<>();
    private final Map<String, MaskingStrategy> strategies = new LinkedHashMap<>();

    public CustomPiiTypeRegistry(PiiProperties properties) {
        for (CustomPiiTypeConfig config : properties.customTypes()) {
            validate(config);
            byName.put(config.name(), config);
            detectors.put(config.name(), new RegexPiiDetector(
                    config.pattern(), config.name(), config.priorityOrDefault(),
                    config.context(), config.requireContextOrDefault(), config.contextWindowOrDefault(),
                    config.checksum()));
            strategies.put(config.name(), new FlexibleMaskingStrategy(config.maskingOrDefault()));
            log.info("Registered custom PII type '{}'", config.name());
        }
    }

    private void validate(CustomPiiTypeConfig config) {
        if (config.name() == null || config.name().isBlank()) {
            throw new IllegalArgumentException("Custom PII type name must not be blank");
        }
        if (byName.containsKey(config.name())) {
            throw new IllegalArgumentException("Duplicate custom PII type name: " + config.name());
        }
        if (config.pattern() == null || config.pattern().isBlank()) {
            throw new IllegalArgumentException("Custom PII type '" + config.name() + "' must have a pattern");
        }
        try {
            Pattern.compile(config.pattern());
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Invalid regex for custom PII type '" + config.name() + "': " + e.getMessage(), e);
        }
        MaskingConfig m = config.maskingOrDefault();
        if (m.keepStart() < 0 || m.keepEnd() < 0) {
            throw new IllegalArgumentException("keep-start/keep-end must be >= 0 for type '" + config.name() + "'");
        }
        if (m.maskChar() == null || m.maskChar().isEmpty()) {
            throw new IllegalArgumentException("mask-char must not be empty for type '" + config.name() + "'");
        }
        if (config.requireContextOrDefault() && config.context().isEmpty()) {
            throw new IllegalArgumentException(
                    "require-context=true requires non-empty context for type '" + config.name() + "'");
        }
        if (config.contextWindowOrDefault() < 0) {
            throw new IllegalArgumentException("context-window must be >= 0 for type '" + config.name() + "'");
        }
        if (config.checksum() != null && !config.checksum().isBlank()
                && ChecksumRegistry.forName(config.checksum()) == null) {
            throw new IllegalArgumentException(
                    "Unknown checksum algorithm '" + config.checksum() + "' for type '" + config.name()
                            + "'. Supported: luhn, inn, snils");
        }
    }

    /**
     * Возвращает все кастомные детекторы (для регистрации как бины {@code PiiDetector}).
     */
    public List<PiiDetector> detectors() {
        return new ArrayList<>(detectors.values());
    }

    /**
     * Возвращает стратегию маскирования для кастомного типа.
     *
     * @param type имя типа
     * @return стратегия или {@code null}, если тип не найден
     */
    public MaskingStrategy strategyFor(String type) {
        return strategies.get(type);
    }

    /**
     * Проверяет, существует ли кастомный тип.
     */
    public boolean contains(String type) {
        return byName.containsKey(type);
    }
}