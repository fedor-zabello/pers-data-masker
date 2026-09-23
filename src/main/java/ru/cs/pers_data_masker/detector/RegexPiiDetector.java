package ru.cs.pers_data_masker.detector;

import ru.cs.pers_data_masker.config.ChecksumRegistry;
import ru.cs.pers_data_masker.domain.Span;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Общий regex-движок для кастомных типов ПД из YAML.
 *
 * <p>Компилирует паттерн из конфигурации и находит спаны. Регистронезависим.
 * Поддерживает опциональную контекстную проверку (если {@code requireContext}
 * включён, фрагмент принимается только при наличии одного из контекстных слов
 * в окне {@code contextWindow}) и проверку контрольной суммы (если задан
 * {@code checksum}).
 */
public class RegexPiiDetector implements PiiDetector {

    private final Pattern pattern;
    private final String type;
    private final int priority;
    private final List<String> context;
    private final boolean requireContext;
    private final int contextWindow;
    private final Predicate<String> checksum;

    public RegexPiiDetector(String regex, String type, int priority) {
        this(regex, type, priority, List.of(), false, 40, null);
    }

    public RegexPiiDetector(String regex, String type, int priority,
                            List<String> context, boolean requireContext, int contextWindow) {
        this(regex, type, priority, context, requireContext, contextWindow, null);
    }

    public RegexPiiDetector(String regex, String type, int priority,
                            List<String> context, boolean requireContext, int contextWindow,
                            String checksum) {
        this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        this.type = type;
        this.priority = priority;
        this.context = context == null ? List.of() : context;
        this.requireContext = requireContext;
        this.contextWindow = contextWindow;
        this.checksum = ChecksumRegistry.forName(checksum);
    }

    @Override
    public List<Span> detect(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        Matcher matcher = pattern.matcher(text);
        List<Span> spans = new ArrayList<>();
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            String original = matcher.group();
            if (requireContext && !hasContext(text, start, end)) {
                continue;
            }
            if (checksum != null && !checksum.test(original)) {
                continue;
            }
            spans.add(new Span(start, end, type, original, confidence(text, start, end)));
        }
        return spans;
    }

    private boolean hasContext(String text, int start, int end) {
        int from = Math.max(0, start - contextWindow);
        int to = Math.min(text.length(), end + contextWindow);
        String around = text.substring(from, to).toLowerCase();
        for (String word : context) {
            if (word != null && !word.isBlank() && around.contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Вычисляет уверенность спана. Если контекст требуется и найден — 0.9,
     * иначе (контекст не требуется) — 1.0.
     */
    protected double confidence(String text, int start, int end) {
        if (requireContext) {
            return hasContext(text, start, end) ? 0.9 : 0.4;
        }
        return 1.0;
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