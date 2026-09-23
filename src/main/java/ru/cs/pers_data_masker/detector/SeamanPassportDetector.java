package ru.cs.pers_data_masker.detector;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.PiiType;

import java.util.regex.Pattern;

/**
 * Детектор серии и номера паспорта моряка.
 *
 * <p>Формат: серия — 2 буквы, номер — 7 цифр (например «АБ 3456789»).
 * Поддерживает разделяющие слова («серия АБ номер 3456789»), пробелы, дефисы
 * и слитное написание, а также нижний регистр. Контекстная проверка на
 * «паспорт моряка»/«моряк»/«удостоверение личности моряка».
 */
@Component
public class SeamanPassportDetector extends AbstractRegexDetector {

    private static final String SEAMAN_REGEX =
            "(?i)(?:серия\\s+)?[А-ЯЁа-яё]{2}(?:\\s+номер\\s+|[\\s\\-]?)\\d{7}";
    private static final Pattern CONTEXT = Pattern.compile(
            "(паспорт\\s*моряка|моряк|удостоверение\\s*личности\\s*моряка|seaman)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public SeamanPassportDetector() {
        super(SEAMAN_REGEX, PiiType.SEAMAN_PASSPORT.name(), 70);
    }

    @Override
    protected boolean accept(String text, int start, int end, String original) {
        int from = Math.max(0, start - 60);
        String before = text.substring(from, start);
        return CONTEXT.matcher(before).find();
    }

    @Override
    protected double confidence(String text, int start, int end, String original) {
        int from = Math.max(0, start - 60);
        String before = text.substring(from, start);
        return CONTEXT.matcher(before).find() ? 0.9 : 0.4;
    }
}