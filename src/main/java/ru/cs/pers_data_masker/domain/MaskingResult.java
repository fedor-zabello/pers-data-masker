package ru.cs.pers_data_masker.domain;

import java.util.List;

/**
 * Результат маскирования: замаскированный текст + список фрагментов
 * соответствия «маска → оригинал» для последующего демаскирования.
 *
 * @param maskedText замаскированный текст
 * @param fragments  фрагменты соответствия (координаты в маске)
 */
public record MaskingResult(String maskedText, List<MaskFragment> fragments) {

    public static MaskingResult empty(String maskedText) {
        return new MaskingResult(maskedText, List.of());
    }
}