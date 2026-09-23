package ru.cs.pers_data_masker.detector;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.PiiType;

/**
 * Детектор адреса регистрации.
 *
 * <p>Ловит фразу «зарегистрирован(а) по адресу», «место регистрации»,
 * «адрес регистрации» и захватывает адрес до конца строки.
 */
@Component
public class RegistrationAddressDetector extends AbstractRegexDetector {

    private static final String REGISTRATION_REGEX =
            "(?i)(?:зарегистрирован(?:а)?\\s+по\\s+адресу|место\\s+регистрации|"
                    + "адрес\\s+регистрации|регистрация\\s*[:\\-]?)\\s*[:\\-]?\\s*"
                    + "[^;\\n]{5,120}";

    public RegistrationAddressDetector() {
        super(REGISTRATION_REGEX, PiiType.REGISTRATION_ADDRESS.name(), 40);
    }
}