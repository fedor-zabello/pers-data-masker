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
 * <p>Спаны сортируются по {@code start} по убыванию и применяются справа налево —
 * так уже обработанные (более правые) позиции не сдвигаются при замене текущего
 * спана переменной длины. Одновременно с заменой вычисляются координаты фрагмента
 * в итоговой строке ({@code maskStart}/{@code maskEnd}) и формируется
 * {@link MaskFragment} для каждого спана.
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
        sorted.sort(Comparator.comparingInt(Span::start).reversed());

        StringBuilder sb = new StringBuilder(text);
        List<MaskFragment> fragments = new ArrayList<>(sorted.size());

        for (Span span : sorted) {
            String masked = typeResolver.strategyFor(span.type()).mask(span.type(), span.original());
            int maskStart = span.start();
            sb.replace(maskStart, span.end(), masked);
            int maskEnd = maskStart + masked.length();
            fragments.add(new MaskFragment(maskStart, maskEnd, span.type(), span.original()));
        }

        fragments.sort(Comparator.comparingInt(MaskFragment::maskStart));
        return new MaskingResult(sb.toString(), fragments);
    }
}