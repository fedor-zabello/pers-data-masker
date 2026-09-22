package ru.cs.pers_data_masker.masking;

import org.springframework.stereotype.Component;
import ru.cs.pers_data_masker.domain.MaskFragment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Восстанавливает исходную строку из маски и фрагментов соответствия.
 *
 * <p>{@code MaskFragment}-ы сортируются по {@code maskStart} по возрастанию;
 * результат собирается слева направо — фрагмент текста маски до {@code maskStart}
 * копируется как есть, далее вставляется {@code original}, далее пропускается до
 * {@code maskEnd}, и так по всем фрагментам до конца строки.
 */
@Component
public class Demasker {

    /**
     * Восстанавливает исходную строку.
     *
     * @param maskedText замаскированный текст
     * @param fragments  фрагменты соответствия (координаты в маске)
     * @return исходная строка
     */
    public String demask(String maskedText, List<MaskFragment> fragments) {
        if (fragments == null || fragments.isEmpty()) {
            return maskedText;
        }
        List<MaskFragment> sorted = new ArrayList<>(fragments);
        sorted.sort(Comparator.comparingInt(MaskFragment::maskStart));

        StringBuilder sb = new StringBuilder(maskedText.length());
        int cursor = 0;
        for (MaskFragment f : sorted) {
            if (f.maskStart() > cursor) {
                sb.append(maskedText, cursor, f.maskStart());
            }
            sb.append(f.original());
            cursor = f.maskEnd();
        }
        if (cursor < maskedText.length()) {
            sb.append(maskedText, cursor, maskedText.length());
        }
        return sb.toString();
    }
}