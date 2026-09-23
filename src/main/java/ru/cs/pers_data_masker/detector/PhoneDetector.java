package ru.cs.pers_data_masker.detector;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.PiiType;

import java.util.regex.Pattern;

/**
 * Детектор номеров телефонов (РФ и международный формат).
 *
 * <p>Требуется либо префикс (+7/8/7), либо наличие разделителей — чтобы не
 * путать с произвольными 10-значными числами (например, ИНН).
 */
@Component
public class PhoneDetector extends AbstractRegexDetector {

    private static final String PHONE_REGEX =
            "(?<![0-9])(?:(?:\\+7|8|7)[\\s\\-.]?)?"
                    + "(?:\\(?\\d{3}\\)?[\\s\\-.]?\\d{3}[\\s\\-.]?\\d{2}[\\s\\-.]?\\d{2})(?![0-9])";
    private static final Pattern SEPARATOR = Pattern.compile("[()\\s\\-.]");

    public PhoneDetector() {
        super(PHONE_REGEX, PiiType.PHONE.name(), 50);
    }

    @Override
    protected boolean accept(String text, int start, int end, String original) {
        boolean hasPrefix = original.startsWith("+7") || original.startsWith("8") || original.startsWith("7");
        boolean hasSeparator = SEPARATOR.matcher(original).find();
        return hasPrefix || hasSeparator;
    }
}