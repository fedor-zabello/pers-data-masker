package ru.cs.pers_data_masker.detector;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.PiiType;

import java.util.regex.Pattern;

/**
 * Детектор серии и номера военного билета.
 *
 * <p>Формат: серия — 2 буквы, номер — 7 цифр (например «АБ 3456789»).
 * Поддерживает разделяющие слова («серия АБ номер 3456789»), пробелы, дефисы
 * и слитное написание, а также нижний регистр. Контекстная проверка на
 * «военный билет»/«военн»/«в/б».
 */
@Component
public class MilitaryIdDetector extends AbstractRegexDetector {

    private static final String MILITARY_REGEX =
            "(?i)(?:серия\\s+)?[А-ЯЁа-яё]{2}(?:\\s+номер\\s+|[\\s\\-]?)\\d{7}";
    private static final Pattern CONTEXT = Pattern.compile(
            "(военн|военный\\s*билет|в/б|удостоверение\\s*военнослужащего)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public MilitaryIdDetector() {
        super(MILITARY_REGEX, PiiType.MILITARY_ID.name(), 70);
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