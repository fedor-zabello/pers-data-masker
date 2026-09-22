package ru.cs.pers_data_masker.detector;

import ru.cs.pers_data_masker.domain.PiiType;

import java.util.regex.Pattern;

/**
 * Базовый детектор дат. Поддерживает числовые форматы (дд.мм.гггг, дд/мм/гггг,
 * дд-мм-гггг, дд мм гггг) и дату текстом («12 января 1990»). Подкласс задаёт
 * контекстную проверку (рождение / выдача).
 */
public abstract class AbstractDateDetector extends AbstractRegexDetector {

    private static final String DATE_REGEX =
            "\\b(?:"
                    + "\\d{1,2}[./\\-\\s]\\d{1,2}[./\\-\\s]\\d{2,4}"
                    + "|\\d{1,2}\\s+(?:января|февраля|марта|апреля|мая|июня|июля|"
                    + "августа|сентября|октября|ноября|декабря)\\s+\\d{4}"
                    + ")\\b";

    private final Pattern context;

    protected AbstractDateDetector(String type, int priority, String contextRegex) {
        super(DATE_REGEX, type, priority);
        this.context = Pattern.compile(contextRegex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    @Override
    protected boolean accept(String text, int start, int end, String original) {
        int from = Math.max(0, start - 40);
        int to = Math.min(text.length(), end + 20);
        String around = text.substring(from, to);
        return context.matcher(around).find();
    }
}