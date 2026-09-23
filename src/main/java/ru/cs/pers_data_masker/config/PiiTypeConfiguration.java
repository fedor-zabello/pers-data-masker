package ru.cs.pers_data_masker.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import ru.cs.pers_data_masker.detector.PiiDetector;
import ru.cs.pers_data_masker.domain.PiiType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Конфигурация типов ПД: регистрирует кастомные детекторы как бины
 * {@link PiiDetector} и валидирует целостность конфигурации (каждый тип в
 * {@code systems.*.types} существует — встроенный ИЛИ кастомный).
 */
@Configuration
public class PiiTypeConfiguration {

    private static final Logger log = LoggerFactory.getLogger(PiiTypeConfiguration.class);

    private final PiiProperties properties;
    private final CustomPiiTypeRegistry customRegistry;

    public PiiTypeConfiguration(PiiProperties properties, CustomPiiTypeRegistry customRegistry) {
        this.properties = properties;
        this.customRegistry = customRegistry;
        validateIntegrity();
    }

private void validateIntegrity() {
        Set<String> builtin = Arrays.stream(PiiType.values())
                .map(Enum::name)
                .collect(Collectors.toSet());
        Set<String> known = new HashSet<>(builtin);
        known.addAll(customRegistry.detectors().stream().map(PiiDetector::type).collect(Collectors.toSet()));

        for (var entry : properties.systems().entrySet()) {
            String system = entry.getKey();
            for (String type : entry.getValue().types()) {
                if (!known.contains(type)) {
                    throw new IllegalArgumentException(
                            "Unknown PII type '" + type + "' in system '" + system
                                    + "'. Must be a built-in type or a custom type from pii.custom-types.");
                }
            }
        }
        log.info("Validated PII type integrity across {} system(s)", properties.systems().size());
    }
}