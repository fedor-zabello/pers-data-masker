package ru.cs.pers_data_masker.detector;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.PiiType;

/**
 * Детектор email-адресов.
 */
@Component
public class EmailDetector extends AbstractRegexDetector {

    private static final String EMAIL_REGEX =
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b";

    public EmailDetector() {
        super(EMAIL_REGEX, PiiType.EMAIL.name(), 60);
    }
}