package ru.cs.pers_data_masker.config;

/**
 * Конфигурация кастомного типа ПД из YAML.
 *
 * @param name     имя типа ПД
 * @param pattern  regex для идентификации
 * @param priority приоритет при пересечении спанов
 * @param masking  правило маскирования
 */
public record CustomPiiTypeConfig(
        String name,
        String pattern,
        Integer priority,
        MaskingConfig masking
) {

    public int priorityOrDefault() {
        return priority == null ? 0 : priority;
    }

    public MaskingConfig maskingOrDefault() {
        return masking == null ? MaskingConfig.defaults() : masking;
    }
}