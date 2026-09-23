package ru.cs.pers_data_masker.detector;

import org.junit.jupiter.api.Test;
import ru.cs.pers_data_masker.domain.Span;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PassportDetectorTest {

    private final PassportDetector detector = new PassportDetector();

    @Test
    void detectsPassportWithSpace() {
        assertDetected("Паспорт 4509 123456", "4509 123456");
    }

    @Test
    void detectsPassportWithDot() {
        assertDetected("Паспорт 4509.123456", "4509.123456");
    }

    @Test
    void ignoresWithoutContext() {
        assertThat(detector.detect("Число 4509 123456")).isEmpty();
    }

    private void assertDetected(String text, String expected) {
        List<Span> spans = detector.detect(text);
        assertThat(spans).isNotEmpty();
        assertThat(spans.get(0).original()).isEqualTo(expected);
        assertThat(spans.get(0).type()).isEqualTo("PASSPORT");
    }
}