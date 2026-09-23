package ru.cs.pers_data_masker.detector;

import org.junit.jupiter.api.Test;
import ru.cs.pers_data_masker.domain.Span;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InnDetectorTest {

    private final InnDetector detector = new InnDetector();

    @Test
    void detectsValid10DigitInn() {
        assertDetected("ИНН 7707083893", "7707083893");
    }

    @Test
    void detectsValid12DigitInn() {
        assertDetected("ИНН 500100732259", "500100732259");
    }

    @Test
    void ignoresInvalidChecksum() {
        assertThat(detector.detect("ИНН 1234567890")).isEmpty();
    }

    private void assertDetected(String text, String expected) {
        List<Span> spans = detector.detect(text);
        assertThat(spans).isNotEmpty();
        assertThat(spans.get(0).original()).isEqualTo(expected);
        assertThat(spans.get(0).type()).isEqualTo("INN");
    }
}