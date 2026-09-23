package ru.cs.pers_data_masker.detector;

import ru.cs.pers_data_masker.domain.Span;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Базовый детектор на основе регулярного выражения.
 *
 * <p>Идентификация не зависит от регистра. Подкласс задаёт {@code pattern} и
 * {@code type}; при необходимости может переопределить {@link #accept} для
 * дополнительной валидации (чек-суммы, словари исключений и т.п.).
 */
public abstract class AbstractRegexDetector implements PiiDetector {

    private final Pattern pattern;
    private final String type;
    private final int priority;

    protected AbstractRegexDetector(String regex, String type, int priority) {
        this.pattern = Pattern.compile(regex,
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS);
        this.type = type;
        this.priority = priority;
    }

    @Override
    public List<Span> detect(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        Matcher matcher = pattern.matcher(text);
        List<Span> spans = new ArrayList<>();
        while (matcher.find()) {
            String original = matcher.group();
            if (accept(text, matcher.start(), matcher.end(), original)) {
                spans.add(new Span(matcher.start(), matcher.end(), type, original,
                        confidence(text, matcher.start(), matcher.end(), original)));
            }
        }
        return spans;
    }

    /**
     * Вычисляет уверенность (0..1) в том, что найденный фрагмент действительно
     * является ПД. По умолчанию — 1.0. Подклассы переопределяют для учёта
     * контекста (наличие контекстных слов повышает, отсутствие — понижает).
     *
     * @param text     полный исходный текст
     * @param start    координата начала фрагмента
     * @param end      координата конца фрагмента
     * @param original найденный фрагмент
     */
    protected double confidence(String text, int start, int end, String original) {
        return 1.0;
    }

    /**
     * Дополнительная валидация найденного фрагмента. По умолчанию — всегда
     * принимает. Подклассы переопределяют для чек-сумм, словарей исключений,
     * контекстных проверок.
     *
     * @param text     полный исходный текст
     * @param start    координата начала фрагмента
     * @param end      координата конца фрагмента
     * @param original найденный фрагмент
     */
    protected boolean accept(String text, int start, int end, String original) {
        return true;
    }

    @Override
    public String type() {
        return type;
    }

    /** Возвращает скомпилированный regex-паттерн (для подклассов с кастомным detect). */
    protected Pattern pattern() {
        return pattern;
    }

    @Override
    public int priority() {
        return priority;
    }
}