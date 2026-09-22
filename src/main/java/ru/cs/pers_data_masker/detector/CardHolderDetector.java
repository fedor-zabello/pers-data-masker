package ru.cs.pers_data_masker.detector;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.PiiType;

import java.util.regex.Pattern;

/**
 * Детектор имени держателя карты (латиницей, как на платёжной карте).
 *
 * <p>Ловит 2 слова латиницей с заглавной буквы с контекстом «card holder»/
 * «держатель»/«holder».
 */
@Component
public class CardHolderDetector extends AbstractRegexDetector {

    private static final String HOLDER_REGEX =
            "\\b[A-Z][a-z]+\\s+[A-Z][a-z]+\\b";
    private static final Pattern CONTEXT = Pattern.compile(
            "(card\\s*holder|holder|держатель\\s*карты|имя\\s*на\\s*карте)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public CardHolderDetector() {
        super(HOLDER_REGEX, PiiType.CARD_HOLDER.name(), 60);
    }

    @Override
    protected boolean accept(String text, int start, int end, String original) {
        int from = Math.max(0, start - 40);
        int to = Math.min(text.length(), end + 20);
        String around = text.substring(from, to);
        return CONTEXT.matcher(around).find();
    }
}