package ru.cs.pers_data_masker.detector;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.PiiType;

import java.util.regex.Pattern;

/**
 * Детектор ИНН физического лица / ИП (ровно 12 цифр) с проверкой контрольных
 * цифр. ИНН юридического лица (10 цифр) персональными данными не является и
 * не детектируется.
 *
 * <p>Контекстное слово («ИНН», «инн», «Inn», «идентификационный номер
 * налогоплательщика» и т.п.) повышает уверенность; без контекста уверенность
 * ниже порога отсечения.
 */
@Component
public class InnDetector extends AbstractRegexDetector {

    private static final String INN_REGEX = "\\b\\d{12}\\b";

    private static final int[] CHECK_12_1 = {7, 2, 4, 10, 3, 5, 9, 4, 6, 8};
    private static final int[] CHECK_12_2 = {3, 7, 2, 4, 10, 3, 5, 9, 4, 6, 8};

    private static final Pattern CONTEXT = Pattern.compile(
            "(идентификационный\\s+номер\\s+налогоплательщика|инн|inn)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public InnDetector() {
        super(INN_REGEX, PiiType.INN.name(), 60);
    }

    @Override
    protected boolean accept(String text, int start, int end, String original) {
        return true;
    }

    @Override
    protected double confidence(String text, int start, int end, String original) {
        int from = Math.max(0, start - 60);
        int to = Math.min(text.length(), end + 20);
        String around = text.substring(from, to);
        boolean hasContext = CONTEXT.matcher(around).find();
        boolean validChecksum = check(original, CHECK_12_1, 10) && check(original, CHECK_12_2, 11);
        if (validChecksum) {
            return hasContext ? 0.9 : 0.6;
        }
        return hasContext ? 0.6 : 0.4;
    }

    private static boolean check(String inn, int[] weights, int checkIndex) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += (inn.charAt(i) - '0') * weights[i];
        }
        int control = sum % 11 % 10;
        return control == (inn.charAt(checkIndex) - '0');
    }
}