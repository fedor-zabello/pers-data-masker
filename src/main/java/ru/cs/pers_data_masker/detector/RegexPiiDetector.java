package ru.cs.pers_data_masker.detector;

import ru.cs.pers_data_masker.domain.Span;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Общий regex-движок для кастомных типов ПД из YAML.
 *
 * <p>Компилирует паттерн из конфигурации и находит спаны. Регистронезависим.
 */
public class RegexPiiDetector implements PiiDetector {

    private final Pattern pattern;
    private final String type;
    private final int priority;

    public RegexPiiDetector(String regex, String type, int priority) {
        this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
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
            spans.add(new Span(matcher.start(), matcher.end(), type, matcher.group()));
        }
        return spans;
    }

    @Override
    public String type() {
        return type;
    }

    @Override
    public int priority() {
        return priority;
    }
}