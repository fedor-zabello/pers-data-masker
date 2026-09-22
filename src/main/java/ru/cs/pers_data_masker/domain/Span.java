package ru.cs.pers_data_masker.domain;

/**
 * Непрерывный фрагмент текста, найденный детектором в исходном тексте.
 *
 * <p>{@code start}/{@code end} — координаты в исходном тексте. Используется
 * детекторами, {@code SpanConflictResolver}-ом и как единица метрики.
 *
 * <p>Тип хранится как {@code String} (имя), чтобы поддерживать и встроенные
 * (enum {@link PiiType}), и кастомные типы из YAML.
 *
 * <p><b>Важно:</b> координаты {@code Span} непригодны для вставки обратно в
 * маску при демаскировании (маска почти всегда другой длины). Для этого
 * используется {@link MaskFragment}.
 *
 * @param start    координата начала в исходном тексте (включительно)
 * @param end      координата конца в исходном тексте (исключительно)
 * @param type     имя типа ПД
 * @param original исходный фрагмент текста
 */
public record Span(int start, int end, String type, String original) {

    public int length() {
        return end - start;
    }
}