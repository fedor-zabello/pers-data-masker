package ru.cs.pers_data_masker.detector;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.PiiType;

import java.util.regex.Pattern;

/**
 * Детектор пин-кода (4 цифры) с контекстной проверкой: рядом должно встречаться
 * слово «пин»/«pin», чтобы не ловить произвольные четырёхзначные числа.
 */
@Component
public class PinDetector extends AbstractRegexDetector {

    private static final String PIN_REGEX = "\\b\\d{4}\\b";
    private static final Pattern CONTEXT = Pattern.compile(
            "(пин|pin)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public PinDetector() {
        super(PIN_REGEX, PiiType.PIN.name(), 80);
    }

    @Override
    protected boolean accept(String text, int start, int end, String original) {
        int from = Math.max(0, start - 30);
        int to = Math.min(text.length(), end + 30);
        String around = text.substring(from, to);
        return CONTEXT.matcher(around).find();
    }

    @Override
    protected double confidence(String text, int start, int end, String original) {
        int from = Math.max(0, start - 30);
        int to = Math.min(text.length(), end + 30);
        String around = text.substring(from, to);
        return CONTEXT.matcher(around).find() ? 0.9 : 0.4;
    }
}