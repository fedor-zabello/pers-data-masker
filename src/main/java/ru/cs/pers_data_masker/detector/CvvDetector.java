package ru.cs.pers_data_masker.detector;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.PiiType;

import java.util.regex.Pattern;

/**
 * Детектор CVV-кода (3 цифры) с контекстной проверкой: перед кодом должно
 * встречаться слово «cvv»/«код»/«security», чтобы не ловить произвольные
 * трёхзначные числа.
 */
@Component
public class CvvDetector extends AbstractRegexDetector {

    private static final String CVV_REGEX = "\\b\\d{3}\\b";
    private static final Pattern CONTEXT = Pattern.compile(
            "(cvv|security\\s*code|код\\s*подтверждения|код\\s*безопасности)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public CvvDetector() {
        super(CVV_REGEX, PiiType.CVV.name(), 80);
    }

    @Override
    protected boolean accept(String text, int start, int end, String original) {
        int from = Math.max(0, start - 40);
        String before = text.substring(from, start);
        return CONTEXT.matcher(before).find();
    }

    @Override
    protected double confidence(String text, int start, int end, String original) {
        int from = Math.max(0, start - 40);
        String before = text.substring(from, start);
        return CONTEXT.matcher(before).find() ? 0.9 : 0.4;
    }
}