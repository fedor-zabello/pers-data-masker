package ru.cs.pers_data_masker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Корневая конфигурация модуля ПД ({@code pii.*}).
 *
 * @param systems     профили систем-потребителей (по имени)
 * @param customTypes кастомные типы ПД из YAML
 */
@ConfigurationProperties(prefix = "pii")
public record PiiProperties(
        Map<String, SystemConfig> systems,
        List<CustomPiiTypeConfig> customTypes
) {

    public PiiProperties {
        if (systems == null) {
            systems = new LinkedHashMap<>();
        }
        if (customTypes == null) {
            customTypes = List.of();
        }
    }
}