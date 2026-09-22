package ru.cs.pers_data_masker.detector;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.PiiType;

/**
 * Детектор номеров телефонов (РФ и международный формат).
 *
 * <p>Поддерживает: {@code +7 (900) 123-45-67}, {@code 8 900 123 45 67},
 * {@code +79001234567}, {@code 900-123-45-67} и т.п.
 */
@Component
public class PhoneDetector extends AbstractRegexDetector {

    private static final String PHONE_REGEX =
            "(?<![0-9])(?:\\+7|8|7)[\\s\\-]?(?:\\(?\\d{3}\\)?[\\s\\-]?\\d{3}[\\s\\-]?\\d{2}[\\s\\-]?\\d{2})(?![0-9])";

    public PhoneDetector() {
        super(PHONE_REGEX, PiiType.PHONE.name(), 50);
    }
}