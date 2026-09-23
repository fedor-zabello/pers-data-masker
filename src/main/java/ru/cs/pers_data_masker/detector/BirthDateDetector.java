package ru.cs.pers_data_masker.detector;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.PiiType;

/**
 * Детектор даты рождения (контекст: «родился»/«рождения»/«дата рождения»).
 */
@Component
public class BirthDateDetector extends AbstractDateDetector {

    public BirthDateDetector() {
        super(PiiType.BIRTH_DATE.name(), 50, "(?i)(родил|рождения|дата\\s*рождения|д\\.р\\.|birth\\s*date|born)");
    }
}