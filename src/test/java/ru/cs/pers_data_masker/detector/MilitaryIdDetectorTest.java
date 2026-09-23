package ru.cs.pers_data_masker.detector;

import org.junit.jupiter.api.Test;
import ru.cs.pers_data_masker.domain.Span;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MilitaryIdDetectorTest {

    private final MilitaryIdDetector detector = new MilitaryIdDetector();

    @Test
    void detectsWithSeriesAndNumberWords() {
        assertDetected("Военный билет серия АБ номер 3456789", "серия АБ номер 3456789");
    }

    @Test
    void detectsWithSpaces() {
        assertDetected("Военный билет АБ 3456789", "АБ 3456789");
    }

    @Test
    void detectsCompact() {
        assertDetected("Военный билет АБ3456789", "АБ3456789");
    }

    @Test
    void detectsWithDash() {
        assertDetected("Военный билет АБ-3456789", "АБ-3456789");
    }

    @Test
    void detectsLowercase() {
        assertDetected("военный билет аб 3456789", "аб 3456789");
    }

    @Test
    void ignoresWithoutContext() {
        assertThat(detector.detect("Номер АБ 3456789")).isEmpty();
    }

    private void assertDetected(String text, String expected) {
        List<Span> spans = detector.detect(text);
        assertThat(spans).isNotEmpty();
        assertThat(spans.get(0).original()).isEqualTo(expected);
        assertThat(spans.get(0).type()).isEqualTo("MILITARY_ID");
    }
}