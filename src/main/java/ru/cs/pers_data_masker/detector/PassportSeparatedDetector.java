package ru.cs.pers_data_masker.detector;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.PiiType;

/**
 * Детектор серии и номера паспорта с разделяющими словами «серия ... номер ...».
 *
 * <p>Пример: «серия 4509 номер 123456». Маскирует оба числа.
 */
@Component
public class PassportSeparatedDetector extends AbstractRegexDetector {

    private static final String PASSPORT_SEP_REGEX =
            "(?i)(?:паспорт\\s+)?серия\\s+(\\d{4})\\s+номер\\s+(\\d{6})";

    public PassportSeparatedDetector() {
        super(PASSPORT_SEP_REGEX, PiiType.PASSPORT.name(), 70);
    }
}