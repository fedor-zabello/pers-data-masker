package ru.cs.pers_data_masker.detector;

import org.junit.jupiter.api.Test;
import ru.cs.pers_data_masker.domain.Span;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CardNumberDetectorTest {

    private final CardNumberDetector detector = new CardNumberDetector();

    @Test
    void detectsValidCardNumber() {
        assertDetected("Карта 4111 1111 1111 1111", "4111 1111 1111 1111");
    }

    @Test
    void detectsCompactCardNumber() {
        assertDetected("Карта 4111111111111111", "4111111111111111");
    }

    @Test
    void ignoresInvalidLuhn() {
        assertThat(detector.detect("Карта 1234 5678 9012 3456")).isEmpty();
    }

    private void assertDetected(String text, String expected) {
        List<Span> spans = detector.detect(text);
        assertThat(spans).isNotEmpty();
        assertThat(spans.get(0).original()).isEqualTo(expected);
        assertThat(spans.get(0).type()).isEqualTo("CARD_NUMBER");
    }
}