package ru.cs.pers_data_masker.detector;

import org.junit.jupiter.api.Test;
import ru.cs.pers_data_masker.domain.Span;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CardHolderDetectorTest {

    private final CardHolderDetector detector = new CardHolderDetector();

    @Test
    void detectsHolderWithContext() {
        assertDetected("Card holder: Ivan Petrov", "Ivan Petrov");
    }

    @Test
    void detectsHolderAllCaps() {
        assertDetected("Card holder: IVAN PETROV", "IVAN PETROV");
    }

    @Test
    void detectsHolderLowercase() {
        assertDetected("Card holder: ivan petrov", "ivan petrov");
    }

    @Test
    void ignoresWithoutContext() {
        assertThat(detector.detect("Ivan Petrov went to the store")).isEmpty();
    }

    private void assertDetected(String text, String expected) {
        List<Span> spans = detector.detect(text);
        assertThat(spans).isNotEmpty();
        assertThat(spans.get(0).original()).isEqualTo(expected);
        assertThat(spans.get(0).type()).isEqualTo("CARD_HOLDER");
    }
}