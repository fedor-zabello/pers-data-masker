package ru.cs.pers_data_masker.detector;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.PiiType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Детектор органа, выдавшего паспорт.
 *
 * <p>Находит фразу после слова «выдан»/«выдано»/«УФМС»/«отделом» до конца
 * предложения (точка, запятая, перенос строки).
 */
@Component
public class PassportIssuerDetector extends AbstractRegexDetector {

    private static final String ISSUER_REGEX =
            "(?i)(?:выдан|выдано|выдал)\\s+([А-ЯЁA-Z][^.,;\\n]{5,120})";

    public PassportIssuerDetector() {
        super(ISSUER_REGEX, PiiType.PASSPORT_ISSUER.name(), 60);
    }

    @Override
    protected boolean accept(String text, int start, int end, String original) {
        return true;
    }
}