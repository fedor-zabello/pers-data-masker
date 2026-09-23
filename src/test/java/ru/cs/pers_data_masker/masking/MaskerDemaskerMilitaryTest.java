package ru.cs.pers_data_masker.masking;

import org.junit.jupiter.api.Test;
import ru.cs.pers_data_masker.domain.MaskFragment;
import ru.cs.pers_data_masker.domain.MaskingResult;
import ru.cs.pers_data_masker.domain.Span;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MaskerDemaskerMilitaryTest {

    private final DefaultMaskingStrategy strategy = new DefaultMaskingStrategy();
    private final Masker masker = new Masker(type -> strategy);
    private final Demasker demasker = new Demasker();

    @Test
    void maskThenUnmaskMilitaryId() {
        String original = "Военный билет АБ 3456789";
        Span span = new Span(14, 23, "MILITARY_ID", "АБ 3456789");
        MaskingResult result = masker.mask(original, List.of(span));
        assertThat(result.maskedText()).isEqualTo("Военный билет АБ *******");
        String restored = demasker.demask(result.maskedText(), result.fragments());
        assertThat(restored).isEqualTo(original);
    }

    @Test
    void maskThenUnmaskSeamanPassport() {
        String original = "Паспорт моряка АБ 3456789";
        Span span = new Span(15, 24, "SEAMAN_PASSPORT", "АБ 3456789");
        MaskingResult result = masker.mask(original, List.of(span));
        assertThat(result.maskedText()).isEqualTo("Паспорт моряка АБ *******");
        String restored = demasker.demask(result.maskedText(), result.fragments());
        assertThat(restored).isEqualTo(original);
    }
}