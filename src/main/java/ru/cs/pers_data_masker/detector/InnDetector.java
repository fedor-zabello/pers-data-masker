package ru.cs.pers_data_masker.detector;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.PiiType;

/**
 * Детектор ИНН (10 или 12 цифр) с проверкой контрольной цифры.
 */
@Component
public class InnDetector extends AbstractRegexDetector {

    private static final String INN_REGEX = "\\b\\d{10}\\b|\\b\\d{12}\\b";

    private static final int[] CHECK_10 = {2, 4, 10, 3, 5, 9, 4, 6, 8};
    private static final int[] CHECK_12_1 = {7, 2, 4, 10, 3, 5, 9, 4, 6, 8};
    private static final int[] CHECK_12_2 = {3, 7, 2, 4, 10, 3, 5, 9, 4, 6, 8};

    public InnDetector() {
        super(INN_REGEX, PiiType.INN.name(), 60);
    }

    @Override
    protected boolean accept(String text, int start, int end, String original) {
        if (original.length() == 10) {
            return check(original, CHECK_10, 9);
        }
        return check(original, CHECK_12_1, 10) && check(original, CHECK_12_2, 11);
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