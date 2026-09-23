package ru.cs.pers_data_masker.detector;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.PiiType;

/**
 * Детектор гражданства: фраза «гражданин/гражданство ...» + название страны.
 *
 * <p>Название страны ограничено словарём (РФ, Россия, Российская Федерация и
 * распространённые страны), чтобы не ловить фамилии («Гражданин Петров»).
 */
@Component
public class CitizenshipDetector extends AbstractRegexDetector {

    private static final String COUNTRIES =
            "РФ|России|Российской\\s+Федерации|Российская\\s+Федерация|Россия|Беларуси|"
                    + "Белоруссии|Казахстана|Украины|Узбекистана|Таджикистана|"
                    + "Киргизии|Кыргызстана|Армении|Азербайджана|Грузии|"
                    + "Молдовы|Молдавии|Латвии|Литвы|Эстонии|Германии|"
                    + "Франции|Италии|Испании|США|Китая|Индии|Турции|"
                    + "Израиля|Финляндии|Польши|Чехии|Сербии|Болгарии";

    private static final String CITIZENSHIP_REGEX =
            "(?i)(?:гражданин|гражданство|гражданка)\\s+(?:" + COUNTRIES + ")";

    public CitizenshipDetector() {
        super(CITIZENSHIP_REGEX, PiiType.CITIZENSHIP.name(), 50);
    }
}