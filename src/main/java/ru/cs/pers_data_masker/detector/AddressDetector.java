package ru.cs.pers_data_masker.detector;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.PiiType;

import java.util.regex.Pattern;

/**
 * Детектор адреса (страна, индекс, город, улица, дом, квартира).
 *
 * <p>Ловит почтовый индекс (6 цифр) с контекстом «г.»/«город»/«ул.»/«улица»/
 * «дом»/«квартира». Исключение — адрес отделения банка (контекст «банк»/«отделение»).
 */
@Component
public class AddressDetector extends AbstractRegexDetector {

    private static final String ADDRESS_REGEX =
            "\\b\\d{6}\\b";
    private static final Pattern CONTEXT = Pattern.compile(
            "(?i)(г\\.|город|ул\\.|улица|дом|квартира|кв\\.|проспект|пр-т|область|обл\\.|край|республика)");
    private static final Pattern BANK_CONTEXT = Pattern.compile(
            "(?i)(банк|отделение\\s*банка|офис\\s*банка|филиал\\s*банка)");

    public AddressDetector() {
        super(ADDRESS_REGEX, PiiType.ADDRESS.name(), 40);
    }

    @Override
    protected boolean accept(String text, int start, int end, String original) {
        int from = Math.max(0, start - 80);
        int to = Math.min(text.length(), end + 40);
        String around = text.substring(from, to);
        if (BANK_CONTEXT.matcher(around).find()) {
            return false;
        }
        return CONTEXT.matcher(around).find();
    }
}