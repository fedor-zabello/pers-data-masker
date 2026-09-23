package ru.cs.pers_data_masker.detector;

import org.junit.jupiter.api.Test;
import ru.cs.pers_data_masker.domain.Span;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneDetectorTest {

    private final PhoneDetector detector = new PhoneDetector();

    @Test
    void detectsWithPlusPrefix() {
        assertDetected("+7 (900) 123-45-67", "+7 (900) 123-45-67");
    }

    @Test
    void detectsWithEightPrefix() {
        assertDetected("8 900 123 45 67", "8 900 123 45 67");
    }

    @Test
    void detectsCompactWithPrefix() {
        assertDetected("+79001234567", "+79001234567");
    }

    @Test
    void detectsWithoutPrefixButWithSeparators() {
        assertDetected("900-123-45-67", "900-123-45-67");
    }

    @Test
    void detectsWithDotSeparators() {
        assertDetected("+7 900.123.45.67", "+7 900.123.45.67");
    }

    @Test
    void ignoresDigitsWithoutPrefixAndSeparators() {
        assertThat(detector.detect("9001234567")).isEmpty();
    }

    @Test
    void ignoresBareSevenPrefix10Digits() {
        assertThat(detector.detect("7707083893")).isEmpty();
    }

    @Test
    void detectsBareSevenPrefix11Digits() {
        assertDetected("79001234567", "79001234567");
    }

    private void assertDetected(String text, String expected) {
        List<Span> spans = detector.detect(text);
        assertThat(spans).isNotEmpty();
        assertThat(spans.get(0).original()).isEqualTo(expected);
        assertThat(spans.get(0).type()).isEqualTo("PHONE");
    }
}