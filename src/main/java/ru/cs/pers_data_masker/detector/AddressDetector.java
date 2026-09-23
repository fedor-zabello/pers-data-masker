package ru.cs.pers_data_masker.detector;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.PiiType;
import ru.cs.pers_data_masker.domain.Span;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Детектор адреса (город, улица, дом, квартира, индекс и т.п.).
 *
 * <p>Два пути распознавания:
 * <ol>
 *   <li>По маркерам адреса (г., город, ул., улица, д., дом, кв., квартира,
 *       проспект, индекс и т.п.) — захватывается до конца строки.</li>
 *   <li>По контекстному триггеру («проживает», «по адресу», «зарегистрирован»
 *       и т.п.) + адрес без маркеров вида «Москва, Ленина 10».</li>
 * </ol>
 *
 * <p>Исключение — адрес отделения банка (контекст «банк»/«отделение»).
 */
@Component
public class AddressDetector extends AbstractRegexDetector {

    private static final String ADDRESS_REGEX =
            "(?i)(?:\\b\\d{6}\\b\\s*)?"
                    + "(?:г\\.|город|ул\\.|улица|проспект|пр-т|переулок|пер\\.|площадь|пл\\.|"
                    + "бульвар|б-р|набережная|наб\\.|шоссе|область|обл\\.|край|республика|"
                    + "район|р-н|посёлок|пос\\.|деревня|дер\\.|село|дом|д\\.|квартира|кв\\.|"
                    + "строение|стр\\.|корпус|корп\\.|индекс|почтовый\\s+индекс)"
                    + "[^;\\n]*";

    private static final String TRIGGER_ADDRESS_REGEX =
            "(?i)(?:проживает|зарегистрирован|зарегистрирована|прописан|прописана|"
                    + "живёт|живет|по\\s+адресу|адрес|находится\\s+по)\\s*[:-]?\\s*"
                    + "([А-ЯЁ][а-яё]+(?:\\s*,\\s*[А-ЯЁ][а-яё]+)*\\s+\\d+(?:[а-яё]|\\s*/\\s*\\d+)?(?:\\s*,\\s*\\d{6})?)";

    private static final Pattern BANK_CONTEXT = Pattern.compile(
            "(?i)(банк|отделение\\s*банка|офис\\s*банка|филиал\\s*банка)");

    private final Pattern triggerPattern;

    public AddressDetector() {
        super(ADDRESS_REGEX, PiiType.ADDRESS.name(), 40);
        this.triggerPattern = Pattern.compile(TRIGGER_ADDRESS_REGEX,
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    @Override
    public List<Span> detect(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        List<Span> spans = new ArrayList<>();
        Matcher matcher = pattern().matcher(text);
        while (matcher.find()) {
            String original = matcher.group();
            if (accept(text, matcher.start(), matcher.end(), original)) {
                spans.add(new Span(matcher.start(), matcher.end(), type(), original));
            }
        }
        Matcher trigger = triggerPattern.matcher(text);
        while (trigger.find()) {
            String addr = trigger.group(1);
            if (addr == null || addr.isEmpty()) {
                continue;
            }
            int start = trigger.start(1);
            int end = trigger.end(1);
            if (accept(text, start, end, addr)) {
                spans.add(new Span(start, end, type(), addr));
            }
        }
        return spans;
    }

    @Override
    protected boolean accept(String text, int start, int end, String original) {
        int from = Math.max(0, start - 80);
        int to = Math.min(text.length(), end + 40);
        String around = text.substring(from, to);
        return !BANK_CONTEXT.matcher(around).find();
    }
}