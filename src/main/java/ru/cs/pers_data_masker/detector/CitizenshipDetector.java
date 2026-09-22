package ru.cs.pers_data_masker.detector;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.PiiType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Детектор гражданства.
 *
 * <p>Ловит фразу «гражданин/гражданство ...» и захватывает название страны.
 */
@Component
public class CitizenshipDetector extends AbstractRegexDetector {

    private static final String CITIZENSHIP_REGEX =
            "(?i)(?:гражданин|гражданство|гражданка)\\s+(?:РФ|России|Российской\\s+Федерации|"
                    + "[А-ЯЁ][а-яё]+)";

    public CitizenshipDetector() {
        super(CITIZENSHIP_REGEX, PiiType.CITIZENSHIP.name(), 50);
    }

    @Override
    protected boolean accept(String text, int start, int end, String original) {
        return true;
    }
}