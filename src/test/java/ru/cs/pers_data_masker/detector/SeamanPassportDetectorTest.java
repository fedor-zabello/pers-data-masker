package ru.cs.pers_data_masker.detector;

import org.junit.jupiter.api.Test;
import ru.cs.pers_data_masker.domain.Span;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SeamanPassportDetectorTest {

    private final SeamanPassportDetector detector = new SeamanPassportDetector();

    @Test
    void detectsWithSeriesAndNumberWords() {
        assertDetected("Паспорт моряка серия АБ номер 3456789", "серия АБ номер 3456789");
    }

    @Test
    void detectsWithSpaces() {
        assertDetected("Паспорт моряка АБ 3456789", "АБ 3456789");
    }

    @Test
    void detectsCompact() {
        assertDetected("Паспорт моряка АБ3456789", "АБ3456789");
    }

    @Test
    void detectsWithDash() {
        assertDetected("Паспорт моряка АБ-3456789", "АБ-3456789");
    }

    @Test
    void detectsLowercase() {
        assertDetected("паспорт моряка аб 3456789", "аб 3456789");
    }

    @Test
    void ignoresWithoutContext() {
        assertThat(detector.detect("Номер АБ 3456789")).isEmpty();
    }

    private void assertDetected(String text, String expected) {
        List<Span> spans = detector.detect(text);
        assertThat(spans).isNotEmpty();
        assertThat(spans.get(0).original()).isEqualTo(expected);
        assertThat(spans.get(0).type()).isEqualTo("SEAMAN_PASSPORT");
    }
}