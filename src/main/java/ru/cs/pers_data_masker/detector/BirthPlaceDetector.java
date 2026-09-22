package ru.cs.pers_data_masker.detector;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.PiiType;

/**
 * Детектор места рождения.
 *
 * <p>Ловит фразу «родился в ...»/«место рождения: ...» и захватывает название
 * населённого пункта.
 */
@Component
public class BirthPlaceDetector extends AbstractRegexDetector {

    private static final String BIRTH_PLACE_REGEX =
            "(?i)(?:родился\\s+в|место\\s+рождения\\s*[:\\-]?\\s*|род\\.\\s*)(?:г\\.\\s*)?"
                    + "[А-ЯЁ][а-яё]+(?:\\s+[А-ЯЁ][а-яё]+)?";

    public BirthPlaceDetector() {
        super(BIRTH_PLACE_REGEX, PiiType.BIRTH_PLACE.name(), 50);
    }
}