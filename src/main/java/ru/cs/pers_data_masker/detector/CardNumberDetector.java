package ru.cs.pers_data_masker.detector;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.PiiType;

/**
 * Детектор номеров платёжных карт (13–19 цифр) с проверкой чек-суммы Луна.
 */
@Component
public class CardNumberDetector extends AbstractRegexDetector {

    private static final String CARD_REGEX =
            "\\b(?:\\d[ -]?){12,18}\\d\\b";

    public CardNumberDetector() {
        super(CARD_REGEX, PiiType.CARD_NUMBER.name(), 70);
    }

    @Override
    protected boolean accept(String text, int start, int end, String original) {
        String digits = original.replaceAll("[^0-9]", "");
        return digits.length() >= 13 && digits.length() <= 19 && luhn(digits);
    }

    private static boolean luhn(String digits) {
        int sum = 0;
        boolean doubleDigit = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (doubleDigit) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
            doubleDigit = !doubleDigit;
        }
        return sum % 10 == 0;
    }
}