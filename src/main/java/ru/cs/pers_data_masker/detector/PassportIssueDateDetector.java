package ru.cs.pers_data_masker.detector;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.PiiType;

/**
 * Детектор даты выдачи паспорта (контекст: «выдан»/«выдачи»/«дата выдачи»).
 */
@Component
public class PassportIssueDateDetector extends AbstractDateDetector {

    public PassportIssueDateDetector() {
        super(PiiType.PASSPORT_ISSUE_DATE.name(), 50, "(?i)(выдан|выдачи|дата\\s*выдачи)");
    }
}