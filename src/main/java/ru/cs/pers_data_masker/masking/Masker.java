package ru.cs.pers_data_masker.masking;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.MaskFragment;
import ru.cs.pers_data_masker.domain.MaskingResult;
import ru.cs.pers_data_masker.domain.Span;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Строит маску из спанов.
 *
 * <p>Спаны сортируются по {@code start} по возрастанию и применяются слева направо.
 * Каждая замена переменной длины сдвигает все последующие позиции, поэтому ведётся
 * накопительный {@code offset} (разница между длиной маски и оригинала всех уже
 * обработанных спанов). Координаты фрагмента в итоговой строке
 * ({@code maskStart}/{@code maskEnd}) вычисляются как {@code span.start() + offset},
 * что гарантирует корректность демаскирования при любом числе спанов.
 */
@Component
public class Masker {

    private final PiiTypeResolver typeResolver;

    public Masker(PiiTypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    public MaskingResult mask(String text, List<Span> spans) {
        if (spans == null || spans.isEmpty()) {
            return MaskingResult.empty(text);
        }
        List<Span> sorted = new ArrayList<>(spans);
        sorted.sort(Comparator.comparingInt(Span::start));

        StringBuilder sb = new StringBuilder(text);
        List<MaskFragment> fragments = new ArrayList<>(sorted.size());
        int offset = 0;

        for (Span span : sorted) {
            String masked = typeResolver.strategyFor(span.type()).mask(span.type(), span.original());
            int maskStart = span.start() + offset;
            sb.replace(maskStart, maskStart + span.original().length(), masked);
            int maskEnd = maskStart + masked.length();
            fragments.add(new MaskFragment(maskStart, maskEnd, span.type(), span.original()));
            offset += masked.length() - span.original().length();
        }

        return new MaskingResult(sb.toString(), fragments);
    }
}