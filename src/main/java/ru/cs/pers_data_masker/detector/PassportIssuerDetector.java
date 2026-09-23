package ru.cs.pers_data_masker.detector;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.PiiType;
import ru.cs.pers_data_masker.domain.Span;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Детектор органа, выдавшего паспорт.
 *
 * <p>Находит фразу после слова «выдан»/«выдано»/«УФМС»/«отделом» до конца
 * предложения (точка, запятая, перенос строки). Точка после сокращений
 * населённых пунктов («г.», «гор.», «пос.» и т.п.) не обрывает захват —
 * следующее слово (название) тоже маскируется.
 */
@Component
public class PassportIssuerDetector extends AbstractRegexDetector {

    private static final String ISSUER_REGEX =
            "(?i)(?:выдан|выдано|выдал)\\s+([А-ЯЁA-Z](?:[^.;\\n]|\\.(?=\\s*[А-ЯЁA-Z])){4,119})";

    public PassportIssuerDetector() {
        super(ISSUER_REGEX, PiiType.PASSPORT_ISSUER.name(), 60);
    }

    @Override
    public List<Span> detect(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        Matcher matcher = pattern().matcher(text);
        List<Span> spans = new ArrayList<>();
        while (matcher.find()) {
            String original = matcher.group(1);
            if (original == null || original.isEmpty()) {
                continue;
            }
            spans.add(new Span(matcher.start(1), matcher.end(1), type(), original,
                    confidence(text, matcher.start(1), matcher.end(1), original)));
        }
        return spans;
    }

    @Override
    protected boolean accept(String text, int start, int end, String original) {
        return true;
    }
}