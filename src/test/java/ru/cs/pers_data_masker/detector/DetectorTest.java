package ru.cs.pers_data_masker.detector;

import org.junit.jupiter.api.Test;
import ru.cs.pers_data_masker.domain.Span;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DetectorTest {

    private final EmailDetector email = new EmailDetector();
    private final PhoneDetector phone = new PhoneDetector();
    private final CardNumberDetector card = new CardNumberDetector();
    private final FullNameDetector fullName = new FullNameDetector();
    private final InnDetector inn = new InnDetector();

    @Test
    void emailDetectsStandardAddress() {
        List<Span> spans = email.detect("contact me at ivan@mail.ru please");
        assertThat(spans).hasSize(1);
        assertThat(spans.get(0).original()).isEqualTo("ivan@mail.ru");
        assertThat(spans.get(0).type()).isEqualTo("EMAIL");
    }

    @Test
    void emailIsCaseInsensitive() {
        List<Span> spans = email.detect("IVAN@MAIL.RU");
        assertThat(spans).hasSize(1);
    }

    @Test
    void phoneDetectsRussianFormat() {
        List<Span> spans = phone.detect("call +7 (900) 123-45-67 now");
        assertThat(spans).hasSize(1);
        assertThat(spans.get(0).original()).isEqualTo("+7 (900) 123-45-67");
    }

    @Test
    void cardDetectsValidLuhn() {
        // 4111 1111 1111 1111 passes Luhn
        List<Span> spans = card.detect("card 4111 1111 1111 1111 here");
        assertThat(spans).hasSize(1);
    }

    @Test
    void cardRejectsInvalidLuhn() {
        List<Span> spans = card.detect("card 1234 5678 9012 3457 here");
        assertThat(spans).isEmpty();
    }

    @Test
    void fullNameDetectsThreeWords() {
        List<Span> spans = fullName.detect("Иванов Иван Иванович пришёл");
        assertThat(spans).hasSize(1);
        assertThat(spans.get(0).original()).isEqualTo("Иванов Иван Иванович");
    }

    @Test
    void fullNameRejectsKnownPerson() {
        List<Span> spans = fullName.detect("поэт Пушкин Александр Сергеевич");
        assertThat(spans).isEmpty();
    }

    @Test
    void innDetectsValid10Digit() {
        // 7707083893 is a valid 10-digit INN
        List<Span> spans = inn.detect("ИНН 7707083893");
        assertThat(spans).hasSize(1);
    }
}