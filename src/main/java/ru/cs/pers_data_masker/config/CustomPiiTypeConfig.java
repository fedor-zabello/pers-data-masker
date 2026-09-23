package ru.cs.pers_data_masker.config;

import java.util.List;

/**
 * Конфигурация кастомного типа ПД из YAML.
 *
 * @param name           имя типа ПД
 * @param pattern        regex для идентификации
 * @param priority       приоритет при пересечении спанов
 * @param masking        правило маскирования
 * @param context        список контекстных слов/фраз (регистронезависимо)
 * @param requireContext если {@code true} — фрагмент принимается только при наличии
 *                       контекста в окне {@code contextWindow}
 * @param contextWindow  размер окна (символов) вокруг фрагмента для поиска контекста
 * @param checksum       имя алгоритма контрольной суммы (luhn, inn, snils) или {@code null}
 */
public record CustomPiiTypeConfig(
        String name,
        String pattern,
        Integer priority,
        MaskingConfig masking,
        List<String> context,
        Boolean requireContext,
        Integer contextWindow,
        String checksum
) {

    public CustomPiiTypeConfig {
        if (context == null) {
            context = List.of();
        }
    }

    public int priorityOrDefault() {
        return priority == null ? 0 : priority;
    }

    public MaskingConfig maskingOrDefault() {
        return masking == null ? MaskingConfig.defaults() : masking;
    }

    public boolean requireContextOrDefault() {
        return requireContext != null && requireContext;
    }

    public int contextWindowOrDefault() {
        return contextWindow == null ? 40 : contextWindow;
    }
}