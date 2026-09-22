package ru.cs.pers_data_masker.masking;

import org.junit.jupiter.api.Test;
import ru.cs.pers_data_masker.domain.MaskFragment;
import ru.cs.pers_data_masker.domain.MaskingResult;
import ru.cs.pers_data_masker.domain.Span;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MaskingTest {

    private final DefaultMaskingStrategy strategy = new DefaultMaskingStrategy();
    private final PiiTypeResolver resolver = type -> strategy;
    private final Masker masker = new Masker(resolver);
    private final Demasker demasker = new Demasker();

    @Test
    void masksFullName() {
        assertThat(strategy.mask("FULL_NAME", "Иванов Иван Иванович")).isEqualTo("И. И. И.");
    }

    @Test
    void masksEmail() {
        assertThat(strategy.mask("EMAIL", "ivan@mail.ru")).isEqualTo("i***@mail.ru");
    }

    @Test
    void masksCardNumber() {
        assertThat(strategy.mask("CARD_NUMBER", "1234 5678 9012 3456")).isEqualTo("1234 **** **** 3456");
    }

    @Test
    void masksInn() {
        assertThat(strategy.mask("INN", "123456789012")).isEqualTo("1234******12");
    }

    @Test
    void maskThenDemaskRoundTrip() {
        String text = "Иванов Иван Иванович, email ivan@mail.ru";
        List<Span> spans = List.of(
                new Span(0, 20, "FULL_NAME", "Иванов Иван Иванович"),
                new Span(28, 39, "EMAIL", "ivan@mail.ru")
        );
        MaskingResult result = masker.mask(text, spans);
        assertThat(result.maskedText()).isEqualTo("И. И. И., email i***@mail.ru");

        String restored = demasker.demask(result.maskedText(), result.fragments());
        assertThat(restored).isEqualTo(text);
    }

    @Test
    void demaskWithNoFragmentsReturnsAsIs() {
        assertThat(demasker.demask("hello", List.of())).isEqualTo("hello");
    }
}