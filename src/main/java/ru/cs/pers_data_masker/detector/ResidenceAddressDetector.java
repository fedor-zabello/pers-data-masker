package ru.cs.pers_data_masker.detector;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.PiiType;

/**
 * Детектор адреса проживания.
 *
 * <p>Ловит фразу «проживает по адресу», «место проживания», «адрес проживания»
 * и захватывает адрес до конца строки.
 */
@Component
public class ResidenceAddressDetector extends AbstractRegexDetector {

    private static final String RESIDENCE_REGEX =
            "(?i)(?:проживает(?:т)?\\s+по\\s+адресу|место\\s+проживания|"
                    + "адрес\\s+проживания|проживание\\s*[:\\-]?)\\s*[:\\-]?\\s*"
                    + "[^;\\n]{5,120}";

    public ResidenceAddressDetector() {
        super(RESIDENCE_REGEX, PiiType.RESIDENCE_ADDRESS.name(), 40);
    }
}