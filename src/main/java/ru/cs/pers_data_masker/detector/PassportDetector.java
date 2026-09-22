package ru.cs.pers_data_masker.detector;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.PiiType;

import java.util.regex.Pattern;

/**
 * Детектор серии и номера паспорта РФ.
 *
 * <p>Формат: серия — 4 цифры, номер — 6 цифр. Поддерживает разделяющие слова
 * («серия 4509 номер 123456») и просто «4509 123456». Контекстная проверка на
 * слово «паспорт»/«серия»/«номер», чтобы не ловить произвольные числа.
 */
@Component
public class PassportDetector extends AbstractRegexDetector {

    private static final String PASSPORT_REGEX =
            "\\b\\d{4}[\\s\\-]?\\d{6}\\b";
    private static final Pattern CONTEXT = Pattern.compile(
            "(?i)(паспорт|серия|номер\\s*паспорта|удостоверение)");

    public PassportDetector() {
        super(PASSPORT_REGEX, PiiType.PASSPORT.name(), 70);
    }

    @Override
    protected boolean accept(String text, int start, int end, String original) {
        int from = Math.max(0, start - 60);
        String before = text.substring(from, start);
        return CONTEXT.matcher(before).find();
    }
}